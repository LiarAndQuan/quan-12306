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

package online.aquan.index12306.biz.ticketservice.service.handler.ticket.filter.purchase;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import online.aquan.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import online.aquan.index12306.biz.ticketservice.service.cache.SeatMarginCacheLoader;
import online.aquan.index12306.framework.starter.cache.DistributedCache;
import online.aquan.index12306.framework.starter.convention.exception.ClientException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static online.aquan.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 购票流程过滤器之验证列车站点库存是否充足
 *
 */
@Component
@RequiredArgsConstructor
public class TrainPurchaseTicketParamStockChainHandler implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO> {

    private final SeatMarginCacheLoader seatMarginCacheLoader;
    private final DistributedCache distributedCache;

    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {
        // 车次站点是否还有余票。如果用户提交多个乘车人非同一座位类型，拆分验证
        String keySuffix = StrUtil.join("_", requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival());
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();
        // 获取用户的座位类型map,key为座位类型,value为相同座位类型的乘客列表
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
            // 获取缓存中的票数,没有的话就查询,一般会命中,因为在购买之前会先查询,查询的时候就已经调用了
            Object stockObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(seatType));
            int stock = Optional.ofNullable(stockObj).map(each -> Integer.parseInt(each.toString())).orElseGet(() -> {
                Map<String, String> seatMarginMap =
                        seatMarginCacheLoader.load(String.valueOf(requestParam.getTrainId()), String.valueOf(seatType), requestParam.getDeparture(), requestParam.getArrival());
                return Optional.ofNullable(seatMarginMap.get(String.valueOf(seatType))).map(Integer::parseInt).orElse(0);
            });
            if (stock >= passengerSeatDetails.size()) {
                return;
            }
            // 如果票不充足
            throw new ClientException("列车站点已无余票");
        });
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
