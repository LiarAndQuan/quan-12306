/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package online.aquan.index12306.biz.ticketservice.service.handler.ticket.filter.query;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.dao.entity.RegionDO;
import online.aquan.index12306.biz.ticketservice.dao.entity.StationDO;
import online.aquan.index12306.biz.ticketservice.dao.mapper.RegionMapper;
import online.aquan.index12306.biz.ticketservice.dao.mapper.StationMapper;
import online.aquan.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import online.aquan.index12306.framework.starter.cache.DistributedCache;
import online.aquan.index12306.framework.starter.convention.exception.ClientException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static online.aquan.index12306.biz.ticketservice.common.constant.RedisKeyConstant.LOCK_QUERY_ALL_REGION_LIST;
import static online.aquan.index12306.biz.ticketservice.common.constant.RedisKeyConstant.QUERY_ALL_REGION_LIST;


/**
 * 查询列车车票流程过滤器之验证数据是否正确
 *
 */
@Component
@RequiredArgsConstructor
public class TrainTicketQueryParamVerifyChainFilter implements TrainTicketQueryChainFilter<TicketPageQueryReqDTO> {

    private final RegionMapper regionMapper;
    private final StationMapper stationMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;

    /**
     * 缓存数据为空并且已经加载过标识
     */
    private static boolean CACHE_DATA_ISNULL_AND_LOAD_FLAG = false;

    @Override
    public void handler(TicketPageQueryReqDTO requestParam) {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        HashOperations<String, Object, Object> hashOperations = stringRedisTemplate.opsForHash();
        //这里根据query_all_region_list这个key找到hash结构中的键值对集合
        //将所有的开始站点和结束站点拼起来,然后查询出他们的value
        List<Object> actualExistList = hashOperations.multiGet(
                QUERY_ALL_REGION_LIST,
                ListUtil.toList(requestParam.getFromStation(), requestParam.getToStation())
        );
        //找到有多少个站点的value为空
        long emptyCount = actualExistList.stream().filter(Objects::isNull).count();
        //如果0个为null,说明都有效
        if (emptyCount == 0L) {
            return;
        }
        //如果有1个为null,那么说明有缓存并且有一个地区不存在
        //如果有两个为null,那么可能是有缓存并且两个地区确实不存在,也有可能是缓存中并不存在这个
        //所以这里的条件就是两个为null&&没有加载缓存&&缓存中不存在这个key
        if (emptyCount == 1L || (emptyCount == 2L && CACHE_DATA_ISNULL_AND_LOAD_FLAG && distributedCache.hasKey(QUERY_ALL_REGION_LIST))) {
            throw new ClientException("出发地或目的地不存在");
        }
        //走到这里说明没有缓存,那我们就需要去数据库中获取一下,先锁住防止重复请求访问数据库
        RLock lock = redissonClient.getLock(LOCK_QUERY_ALL_REGION_LIST);
        lock.lock();
        try {
            //双重判定锁
            if (distributedCache.hasKey(QUERY_ALL_REGION_LIST)) {
                actualExistList = hashOperations.multiGet(
                        QUERY_ALL_REGION_LIST,
                        ListUtil.toList(requestParam.getFromStation(), requestParam.getToStation())
                );
                emptyCount = actualExistList.stream().filter(Objects::nonNull).count();
                if (emptyCount != 2L) {
                    throw new ClientException("出发地或目的地不存在");
                }
                return;
            }
            //这里才是真正查询数据库的地方
            List<RegionDO> regionDOList = regionMapper.selectList(Wrappers.emptyWrapper());
            List<StationDO> stationDOList = stationMapper.selectList(Wrappers.emptyWrapper());
            HashMap<Object, Object> regionValueMap = Maps.newHashMap();
            //地区的code和名字
            for (RegionDO each : regionDOList) {
                regionValueMap.put(each.getCode(), each.getName());
            }
            //站点的code和名字
            for (StationDO each : stationDOList) {
                regionValueMap.put(each.getCode(), each.getName());
            }
            //将所有的都放进去
            hashOperations.putAll(QUERY_ALL_REGION_LIST, regionValueMap);
            CACHE_DATA_ISNULL_AND_LOAD_FLAG = true;
            //再次执行逻辑判断
            emptyCount = regionValueMap.keySet().stream()
                    .filter(each -> StrUtil.equalsAny(each.toString(), requestParam.getFromStation(), requestParam.getToStation()))
                    .count();
            if (emptyCount != 2L) {
                throw new ClientException("出发地或目的地不存在");
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
