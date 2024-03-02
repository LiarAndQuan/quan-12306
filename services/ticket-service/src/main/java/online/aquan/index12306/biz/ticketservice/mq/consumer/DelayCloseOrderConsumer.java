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

package online.aquan.index12306.biz.ticketservice.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.aquan.index12306.biz.ticketservice.common.constant.TicketRocketMQConstant;
import online.aquan.index12306.biz.ticketservice.dto.domain.RouteDTO;
import online.aquan.index12306.biz.ticketservice.dto.req.CancelTicketOrderReqDTO;
import online.aquan.index12306.biz.ticketservice.mq.domain.MessageWrapper;
import online.aquan.index12306.biz.ticketservice.mq.event.DelayCloseOrderEvent;
import online.aquan.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import online.aquan.index12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import online.aquan.index12306.biz.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import online.aquan.index12306.biz.ticketservice.service.SeatService;
import online.aquan.index12306.biz.ticketservice.service.TrainStationService;
import online.aquan.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import online.aquan.index12306.biz.ticketservice.service.handler.ticket.tokenbucket.TicketAvailabilityTokenBucket;
import online.aquan.index12306.framework.starter.cache.DistributedCache;
import online.aquan.index12306.framework.starter.common.toolkit.BeanUtil;
import online.aquan.index12306.framework.starter.convention.result.Result;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static online.aquan.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;


/**
 * 延迟关闭订单消费者
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = TicketRocketMQConstant.ORDER_DELAY_CLOSE_TOPIC_KEY,
        selectorExpression = TicketRocketMQConstant.ORDER_DELAY_CLOSE_TAG_KEY,
        consumerGroup = TicketRocketMQConstant.TICKET_DELAY_CLOSE_CG_KEY
)
public final class DelayCloseOrderConsumer implements RocketMQListener<MessageWrapper<DelayCloseOrderEvent>> {

    private final SeatService seatService;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final TrainStationService trainStationService;
    private final DistributedCache distributedCache;
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;

    @Value("${ticket.availability.cache-update.type:}")
    private String ticketAvailabilityCacheUpdateType;

    @Override
    public void onMessage(MessageWrapper<DelayCloseOrderEvent> delayCloseOrderEventMessageWrapper) {
        log.info("[延迟关闭订单] 开始消费：{}", JSON.toJSONString(delayCloseOrderEventMessageWrapper));
        //获取到携带的消息
        DelayCloseOrderEvent delayCloseOrderEvent = delayCloseOrderEventMessageWrapper.getMessage();
        //获取到订单号
        String orderSn = delayCloseOrderEvent.getOrderSn();
        Result<Boolean> closedTickOrder;
        try {
            //调用订单模块关闭订单
            closedTickOrder = ticketOrderRemoteService.closeTickOrder(new CancelTicketOrderReqDTO(orderSn));
        } catch (Throwable ex) {
            log.error("[延迟关闭订单] 订单号：{} 远程调用订单服务失败", orderSn, ex);
            throw ex;
        }
        //如果成功关闭,那么需要更新余票缓存
        if (closedTickOrder.isSuccess() && !StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog")) {
            //如果用户已经支付了那么直接返回就好了
            if (!closedTickOrder.getData()) {
                log.info("[延迟关闭订单] 订单号：{} 用户已支付订单", orderSn);
                return;
            }
            String trainId = delayCloseOrderEvent.getTrainId();
            String departure = delayCloseOrderEvent.getDeparture();
            String arrival = delayCloseOrderEvent.getArrival();
            List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults = delayCloseOrderEvent.getTrainPurchaseTicketResults();
            try {
                //将两个站点中间的那些站点的票解锁,因为在购买的时候我们锁定了那些票导致不可用
                seatService.unlock(trainId, departure, arrival, trainPurchaseTicketResults);
            } catch (Throwable ex) {
                log.error("[延迟关闭订单] 订单号：{} 回滚列车DB座位状态失败", orderSn, ex);
                throw ex;
            }
            try {
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                //将座位类型来分类
                Map<Integer, List<TrainPurchaseTicketRespDTO>> seatTypeMap = trainPurchaseTicketResults.stream()
                        .collect(Collectors.groupingBy(TrainPurchaseTicketRespDTO::getSeatType));
                //获取需列车站点扣减路线关系,获取开始站点和目的站点、中间站点以及关联站点信息
                List<RouteDTO> routeDTOList = trainStationService.listTakeoutTrainStationRoute(trainId, departure, arrival);
                routeDTOList.forEach(each -> {
                    //找到票的缓存的key
                    String keySuffix = StrUtil.join("_", trainId, each.getStartStation(), each.getEndStation());
                    seatTypeMap.forEach((seatType, trainPurchaseTicketRespDTOList) -> {
                        //增加缓存,增加的数量就是这个座位类型有多少张票
                        stringRedisTemplate.opsForHash()
                                .increment(TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(seatType), trainPurchaseTicketRespDTOList.size());
                    });
                });
                //todo 注释
                TicketOrderDetailRespDTO ticketOrderDetail = BeanUtil.convert(delayCloseOrderEvent, TicketOrderDetailRespDTO.class);
                ticketOrderDetail.setPassengerDetails(BeanUtil.convert(delayCloseOrderEvent.getTrainPurchaseTicketResults(), TicketOrderPassengerDetailRespDTO.class));
                ticketAvailabilityTokenBucket.rollbackInBucket(ticketOrderDetail);
            } catch (Throwable ex) {
                log.error("[延迟关闭订单] 订单号：{} 回滚列车Cache余票失败", orderSn, ex);
                throw ex;
            }
        }
    }
}
