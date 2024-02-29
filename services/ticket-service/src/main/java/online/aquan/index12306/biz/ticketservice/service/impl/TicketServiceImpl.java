package online.aquan.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.common.enums.TicketChainMarkEnum;
import online.aquan.index12306.biz.ticketservice.dao.entity.StationDO;
import online.aquan.index12306.biz.ticketservice.dao.entity.TrainDO;
import online.aquan.index12306.biz.ticketservice.dao.entity.TrainStationPriceDO;
import online.aquan.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import online.aquan.index12306.biz.ticketservice.dao.mapper.StationMapper;
import online.aquan.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import online.aquan.index12306.biz.ticketservice.dao.mapper.TrainStationPriceMapper;
import online.aquan.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import online.aquan.index12306.biz.ticketservice.dto.domain.SeatClassDTO;
import online.aquan.index12306.biz.ticketservice.dto.domain.TicketListDTO;
import online.aquan.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import online.aquan.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import online.aquan.index12306.biz.ticketservice.service.TicketService;
import online.aquan.index12306.biz.ticketservice.service.cache.SeatMarginCacheLoader;
import online.aquan.index12306.biz.ticketservice.toolkit.DateUtil;
import online.aquan.index12306.biz.ticketservice.toolkit.TimeStringComparator;
import online.aquan.index12306.framework.starter.cache.DistributedCache;
import online.aquan.index12306.framework.starter.cache.toolkit.CacheUtil;
import online.aquan.index12306.framework.starter.designpattern.chain.AbstractChainContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static online.aquan.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static online.aquan.index12306.biz.ticketservice.common.constant.RedisKeyConstant.*;
import static online.aquan.index12306.biz.ticketservice.toolkit.DateUtil.convertDateToLocalTime;


@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    
    private final AbstractChainContext<TicketPageQueryReqDTO> ticketPageQueryAbstractChainContext;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;
    private final StationMapper stationMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final TrainMapper trainMapper;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final SeatMarginCacheLoader seatMarginCacheLoader;

    @Override
    public TicketPageQueryRespDTO pageListTicketQueryV1(TicketPageQueryReqDTO requestParam) {
        // 责任链模式 验证城市名称是否存在、不存在加载缓存以及出发日期不能小于当前日期等等
        ticketPageQueryAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_QUERY_FILTER.name(), requestParam);
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        //通过缓存中的站点和地区的映射找到开始地区和结束地区
        List<Object> stationDetails = stringRedisTemplate.opsForHash()
                .multiGet(REGION_TRAIN_STATION_MAPPING, Lists.newArrayList(requestParam.getFromStation(), requestParam.getToStation()));
        long count = stationDetails.stream().filter(Objects::isNull).count();
        //如果count!=0,那么说明缓存中不存在这个映射,需要手动查询
        if (count > 0) {
            RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION_MAPPING);
            lock.lock();
            try {
                //双重判定
                stationDetails = stringRedisTemplate.opsForHash()
                        .multiGet(REGION_TRAIN_STATION_MAPPING, Lists.newArrayList(requestParam.getFromStation(), requestParam.getToStation()));
                count = stationDetails.stream().filter(Objects::isNull).count();
                //如果还是!=0,那么就需要更新缓存了
                if (count > 0) {
                    //找到所有的车站code和地区名字
                    List<StationDO> stationDOList = stationMapper.selectList(Wrappers.emptyWrapper());
                    Map<String, String> regionTrainStationMap = new HashMap<>();
                    stationDOList.forEach(each -> regionTrainStationMap.put(each.getCode(), each.getRegionName()));
                    stringRedisTemplate.opsForHash().putAll(REGION_TRAIN_STATION_MAPPING, regionTrainStationMap);
                    //将开始地区与结束地区保存下来
                    stationDetails = new ArrayList<>();
                    stationDetails.add(regionTrainStationMap.get(requestParam.getFromStation()));
                    stationDetails.add(regionTrainStationMap.get(requestParam.getToStation()));
                }
            } finally {
                lock.unlock();
            }
        }
        //接着就是查询所有满足条件的列车的基本信息
        List<TicketListDTO> seatResults = new ArrayList<>();
        //列车详细查询，Key Prefix + 起始城市_终点城市_日期
        String buildRegionTrainStationHashKey = String.format(REGION_TRAIN_STATION, stationDetails.get(0), stationDetails.get(1));
        //从缓存中看看是否有列车基本信息
        Map<Object, Object> regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);
        //如果这个为空,那么需要查询数据库
        if (MapUtil.isEmpty(regionTrainStationAllMap)) {
            RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION);
            lock.lock();
            try {
                //双重判定
                regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);
                if (MapUtil.isEmpty(regionTrainStationAllMap)) {
                    //通过relation表可以查到列车id,出发时间,到达时间等
                    LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                            .eq(TrainStationRelationDO::getStartRegion, stationDetails.get(0))
                            .eq(TrainStationRelationDO::getEndRegion, stationDetails.get(1));
                    List<TrainStationRelationDO> trainStationRelationList = trainStationRelationMapper.selectList(queryWrapper);
                    for (TrainStationRelationDO each : trainStationRelationList) {
                        //根据列车id在缓存中查询列车的基本信息
                        TrainDO trainDO = distributedCache.safeGet(
                                TRAIN_INFO + each.getTrainId(),
                                TrainDO.class,
                                () -> trainMapper.selectById(each.getTrainId()),
                                ADVANCE_TICKET_DAY,
                                TimeUnit.DAYS);
                        //根据trainDo和each(开始时间,结束时间)就可以组成列车的基本信息
                        TicketListDTO result = new TicketListDTO();
                        result.setTrainId(String.valueOf(trainDO.getId()));
                        result.setTrainNumber(trainDO.getTrainNumber());
                        result.setDepartureTime(convertDateToLocalTime(each.getDepartureTime(), "HH:mm"));
                        result.setArrivalTime(convertDateToLocalTime(each.getArrivalTime(), "HH:mm"));
                        result.setDuration(DateUtil.calculateHourDifference(each.getDepartureTime(), each.getArrivalTime()));
                        result.setDeparture(each.getDeparture());
                        result.setArrival(each.getArrival());
                        result.setDepartureFlag(each.getDepartureFlag());
                        result.setArrivalFlag(each.getArrivalFlag());
                        result.setTrainType(trainDO.getTrainType());
                        result.setTrainBrand(trainDO.getTrainBrand());
                        if (StrUtil.isNotBlank(trainDO.getTrainTag())) {
                            result.setTrainTags(StrUtil.split(trainDO.getTrainTag(), ","));
                        }
                        long betweenDay = cn.hutool.core.date.DateUtil.betweenDay(each.getDepartureTime(), each.getArrivalTime(), false);
                        result.setDaysArrived((int) betweenDay);
                        result.setSaleStatus(new Date().after(trainDO.getSaleTime()) ? 0 : 1);
                        result.setSaleTime(convertDateToLocalTime(trainDO.getSaleTime(), "MM-dd HH:mm"));
                        seatResults.add(result);
                        //构建出缓存列车信息的key(起始站点+终止站点)并且将列车信息放入缓存中
                        regionTrainStationAllMap.put(CacheUtil.buildKey(String.valueOf(each.getTrainId()), each.getDeparture(), each.getArrival()), JSON.toJSONString(result));
                    }
                    stringRedisTemplate.opsForHash().putAll(buildRegionTrainStationHashKey, regionTrainStationAllMap);
                }
            } finally {
                lock.unlock();
            }
        }
        //如果seatResults为null那么说明是走的缓存,还没有赋值,需要转化赋值
        seatResults = CollUtil.isEmpty(seatResults)
                ? regionTrainStationAllMap.values().stream().map(each -> JSON.parseObject(each.toString(), TicketListDTO.class)).toList()
                : seatResults;
        //根据出发时间排序
        seatResults = seatResults.stream().sorted(new TimeStringComparator()).toList();
        //对于每一辆列车
        for (TicketListDTO each : seatResults) {
            //都需要获取列车不同座位的价格
            String trainStationPriceStr = distributedCache.safeGet(
                    String.format(TRAIN_STATION_PRICE, each.getTrainId(), each.getDeparture(), each.getArrival()),
                    String.class,
                    () -> {
                        LambdaQueryWrapper<TrainStationPriceDO> trainStationPriceQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                                .eq(TrainStationPriceDO::getDeparture, each.getDeparture())
                                .eq(TrainStationPriceDO::getArrival, each.getArrival())
                                .eq(TrainStationPriceDO::getTrainId, each.getTrainId());
                        return JSON.toJSONString(trainStationPriceMapper.selectList(trainStationPriceQueryWrapper));
                    },
                    ADVANCE_TICKET_DAY,
                    TimeUnit.DAYS
            );
            //得到了每辆列车不同的座位的价格
            List<TrainStationPriceDO> trainStationPriceDOList = JSON.parseArray(trainStationPriceStr, TrainStationPriceDO.class);
            List<SeatClassDTO> seatClassList = new ArrayList<>();
            //对于每一种不同的座位
            trainStationPriceDOList.forEach(item -> {
                String seatType = String.valueOf(item.getSeatType());
                String keySuffix = StrUtil.join("_", each.getTrainId(), item.getDeparture(), item.getArrival());
                //获取到这种座位的数量
                Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, seatType);
                int quantity = Optional.ofNullable(quantityObj)
                        .map(Object::toString)
                        .map(Integer::parseInt)
                        .orElseGet(() -> {
                            //如果为空的话那么说明缓存中没有,需要加载出来
                            Map<String, String> seatMarginMap = seatMarginCacheLoader.load(String.valueOf(each.getTrainId()), seatType, item.getDeparture(), item.getArrival());
                            return Optional.ofNullable(seatMarginMap.get(String.valueOf(item.getSeatType()))).map(Integer::parseInt).orElse(0);
                        });
                seatClassList.add(new SeatClassDTO(item.getSeatType(), quantity, new BigDecimal(item.getPrice()).divide(new BigDecimal("100"), 1, RoundingMode.HALF_UP), false));
            });
            //将座位信息加入result中
            each.setSeatClassList(seatClassList);
        }
        return TicketPageQueryRespDTO.builder()
                .trainList(seatResults)
                //出发站点集合
                .departureStationList(buildDepartureStationList(seatResults))
                //到达站点集合
                .arrivalStationList(buildArrivalStationList(seatResults))
                //列车类型集合
                .trainBrandList(buildTrainBrandList(seatResults))
                //座位类型集合
                .seatClassTypeList(buildSeatClassList(seatResults))
                .build();
    }

    private List<String> buildDepartureStationList(List<TicketListDTO> seatResults) {
        return seatResults.stream().map(TicketListDTO::getDeparture).distinct().collect(Collectors.toList());
    }

    private List<String> buildArrivalStationList(List<TicketListDTO> seatResults) {
        return seatResults.stream().map(TicketListDTO::getArrival).distinct().collect(Collectors.toList());
    }

    private List<Integer> buildSeatClassList(List<TicketListDTO> seatResults) {
        Set<Integer> resultSeatClassList = new HashSet<>();
        for (TicketListDTO each : seatResults) {
            for (SeatClassDTO item : each.getSeatClassList()) {
                resultSeatClassList.add(item.getType());
            }
        }
        return resultSeatClassList.stream().toList();
    }

    private List<Integer> buildTrainBrandList(List<TicketListDTO> seatResults) {
        Set<Integer> trainBrandSet = new HashSet<>();
        for (TicketListDTO each : seatResults) {
            if (StrUtil.isNotBlank(each.getTrainBrand())) {
                trainBrandSet.addAll(StrUtil.split(each.getTrainBrand(), ",").stream().map(Integer::parseInt).toList());
            }
        }
        return trainBrandSet.stream().toList();
    }


}
