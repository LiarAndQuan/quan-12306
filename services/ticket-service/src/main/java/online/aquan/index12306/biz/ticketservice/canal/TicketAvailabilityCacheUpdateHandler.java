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

package online.aquan.index12306.biz.ticketservice.canal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import online.aquan.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import online.aquan.index12306.biz.ticketservice.mq.event.CanalBinlogEvent;
import online.aquan.index12306.framework.starter.cache.DistributedCache;
import online.aquan.index12306.framework.starter.designpattern.strategy.AbstractExecuteStrategy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

import static online.aquan.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;


/**
 * 列车余票缓存更新策略
 * 监听t_seat表的变化
 *
 */
@Component
@RequiredArgsConstructor
public class TicketAvailabilityCacheUpdateHandler implements AbstractExecuteStrategy<CanalBinlogEvent, Void> {

    private final DistributedCache distributedCache;

    @Override
    public void execute(CanalBinlogEvent message) {
        List<Map<String, Object>> messageDataList = new ArrayList<>();
        List<Map<String, Object>> actualOldDataList = new ArrayList<>();
        //遍历改变之前的数据
        for (int i = 0; i < message.getOld().size(); i++) {
            //是一个map结构,里面存储了变更的字段名和旧值
            Map<String, Object> oldDataMap = message.getOld().get(i);
            //如果找到了seat_status的变更
            if (oldDataMap.get("seat_status") != null && StrUtil.isNotBlank(oldDataMap.get("seat_status").toString())) {
                //获取到这次变更造成的新数据,因为消息里面都是List<Map<>>结构的
                Map<String, Object> currentDataMap = message.getData().get(i);
                //如果旧的数据中是座位可售卖的类型或者是被锁住的类型
                if (StrUtil.equalsAny(currentDataMap.get("seat_status").toString(), String.valueOf(SeatStatusEnum.AVAILABLE.getCode()), String.valueOf(SeatStatusEnum.LOCKED.getCode()))) {
                    //那么记录下来旧的这条数据
                    actualOldDataList.add(oldDataMap);
                    messageDataList.add(currentDataMap);
                }
            }
        }
        if (CollUtil.isEmpty(messageDataList) || CollUtil.isEmpty(actualOldDataList)) {
            return;
        }
        Map<String, Map<Integer, Integer>> cacheChangeKeyMap = new HashMap<>();
        //遍历是因为监听传给消息队列的数据都是List<Map<>>类型的,为了兼容多条数据改变
        for (int i = 0; i < messageDataList.size(); i++) {
            Map<String, Object> each = messageDataList.get(i);
            Map<String, Object> actualOldData = actualOldDataList.get(i);
            //获取座位类型,判断是否是加缓存还是减缓存
            //如果之前状态为0,代表可售,那么就是变为不可售,需要减缓存
            String seatStatus = actualOldData.get("seat_status").toString();
            int increment = Objects.equals(seatStatus, "0") ? -1 : 1;
            String trainId = each.get("train_id").toString();
            //获取缓存的key
            String hashCacheKey = TRAIN_STATION_REMAINING_TICKET + trainId + "_" + each.get("start_station") + "_" + each.get("end_station");
            //用于暂时记录座位类型的缓存
            Map<Integer, Integer> seatTypeMap = cacheChangeKeyMap.get(hashCacheKey);
            //如果为空那么建立
            if (CollUtil.isEmpty(seatTypeMap)) {
                seatTypeMap = new HashMap<>();
            }
            Integer seatType = Integer.parseInt(each.get("seat_type").toString());
            Integer num = seatTypeMap.get(seatType);
            //更新map中的座位数量
            seatTypeMap.put(seatType, num == null ? increment : num + increment);
            cacheChangeKeyMap.put(hashCacheKey, seatTypeMap);
        }
        StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
        //遍历这个map{key:{座位类型:变更数量}},然后更新缓存就好了
        cacheChangeKeyMap.forEach((cacheKey, cacheVal) -> cacheVal.forEach((seatType, num) -> instance.opsForHash().increment(cacheKey, String.valueOf(seatType), num)));
    }

    @Override
    public String mark() {
        return CanalExecuteStrategyMarkEnum.T_SEAT.getActualTable();
    }
}
