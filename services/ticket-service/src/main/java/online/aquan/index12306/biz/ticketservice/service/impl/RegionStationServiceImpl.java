package online.aquan.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.common.enums.RegionStationQueryTypeEnum;
import online.aquan.index12306.biz.ticketservice.dao.entity.RegionDO;
import online.aquan.index12306.biz.ticketservice.dao.entity.StationDO;
import online.aquan.index12306.biz.ticketservice.dao.mapper.RegionMapper;
import online.aquan.index12306.biz.ticketservice.dao.mapper.StationMapper;
import online.aquan.index12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import online.aquan.index12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;
import online.aquan.index12306.biz.ticketservice.service.RegionStationService;
import online.aquan.index12306.framework.starter.cache.DistributedCache;
import online.aquan.index12306.framework.starter.cache.core.CacheLoader;
import online.aquan.index12306.framework.starter.cache.toolkit.CacheUtil;
import online.aquan.index12306.framework.starter.common.enums.FlagEnum;
import online.aquan.index12306.framework.starter.common.toolkit.BeanUtil;
import online.aquan.index12306.framework.starter.convention.exception.ClientException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static online.aquan.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static online.aquan.index12306.biz.ticketservice.common.constant.RedisKeyConstant.LOCK_QUERY_REGION_STATION_LIST;
import static online.aquan.index12306.biz.ticketservice.common.constant.RedisKeyConstant.REGION_STATION;

@Service
@RequiredArgsConstructor
public class RegionStationServiceImpl implements RegionStationService {
    
    private final StationMapper stationMapper;
    private final RegionMapper regionMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;

    @Override
    public List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO requestParam) {
        String key;
        //如果传入了车站名,那么直接根据名字查询对应的站点即可
        if (StrUtil.isNotBlank(requestParam.getName())) {
            key  = REGION_STATION  + requestParam.getName();
            return safeGetRegionStation(
                    key ,
                    //这里的就是传入了缓存不存在时的调用逻辑,只有不存在时会被调用然后放入缓存并返回这个数据
                    () -> {
                        LambdaQueryWrapper<StationDO> queryWrapper = Wrappers.lambdaQuery(StationDO.class)
                                .likeRight(StationDO::getName, requestParam.getName())
                                .or()
                                .likeRight(StationDO::getSpell, requestParam.getName());
                        List<StationDO> stationDOList = stationMapper.selectList(queryWrapper);
                        return JSON.toJSONString(BeanUtil.convert(stationDOList, RegionStationQueryRespDTO.class));
                    },
                    requestParam.getName()
            );
        }
        //没有传入车站名,那么就看前端传入的type字段来查询地区,并且更新key值
        key  = REGION_STATION  + requestParam.getQueryType();
        LambdaQueryWrapper<RegionDO> queryWrapper = switch (requestParam.getQueryType()) {
            //如果为0,那么查询热门地区,否则根据映射关系查询对应首字母的站点并返回
            case 0 -> Wrappers.lambdaQuery(RegionDO.class)
                    .eq(RegionDO::getPopularFlag, FlagEnum.TRUE.code());
            case 1 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.A_E.getSpells());
            case 2 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.F_J.getSpells());
            case 3 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.K_O.getSpells());
            case 4 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.P_T.getSpells());
            case 5 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.U_Z.getSpells());
            default -> throw new ClientException("查询失败，请检查查询参数是否正确");
        };
        return safeGetRegionStation(
                key,
                () -> {
                    List<RegionDO> regionDOList = regionMapper.selectList(queryWrapper);
                    return JSON.toJSONString(BeanUtil.convert(regionDOList, RegionStationQueryRespDTO.class));
                },
                String.valueOf(requestParam.getQueryType())
        );
    }

    private  List<RegionStationQueryRespDTO> safeGetRegionStation(final String key, CacheLoader<String> loader, String param) {
        List<RegionStationQueryRespDTO> result;
        //如果缓存中可以找到地区,那么直接返回就可以了
        if (CollUtil.isNotEmpty(result = JSON.parseArray(distributedCache.get(key, String.class), RegionStationQueryRespDTO.class))) {
            return result;
        }
        //否则的话就加锁,因为如果大量请求都重复请求这个接口那么会出问题,最好是加锁之后后续查询直接走缓存
        String lockKey = String.format(LOCK_QUERY_REGION_STATION_LIST, param);
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            //这里采取二次缓存判断
            if (CollUtil.isEmpty(result = JSON.parseArray(distributedCache.get(key, String.class), RegionStationQueryRespDTO.class))) {
                //如果还是为空,那么调用loadAndSet方法从数据库中获取出真正的数据
                if (CollUtil.isEmpty(result = loadAndSet(key, loader))) {
                    return Collections.emptyList();
                }
            }
        } finally {
            lock.unlock();
        }
        return result;
    }

    private List<RegionStationQueryRespDTO> loadAndSet(final String key, CacheLoader<String> loader) {
        //这里的loader.load()方法就是传入了从数据库中查找数据的方法
        String result = loader.load();
        if (CacheUtil.isNullOrBlank(result)) {
            return Collections.emptyList();
        }
        //查询之后将其保存在缓存中
        List<RegionStationQueryRespDTO> respDTOList = JSON.parseArray(result, RegionStationQueryRespDTO.class);
        distributedCache.put(
                key,
                result,
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );
        return respDTOList;
    }
}
