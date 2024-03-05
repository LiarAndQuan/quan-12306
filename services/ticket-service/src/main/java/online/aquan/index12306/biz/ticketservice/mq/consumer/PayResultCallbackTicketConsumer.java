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

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.aquan.index12306.biz.ticketservice.common.constant.TicketRocketMQConstant;
import online.aquan.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import online.aquan.index12306.biz.ticketservice.dao.entity.SeatDO;
import online.aquan.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import online.aquan.index12306.biz.ticketservice.mq.domain.MessageWrapper;
import online.aquan.index12306.biz.ticketservice.mq.event.PayResultCallbackTicketEvent;
import online.aquan.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import online.aquan.index12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import online.aquan.index12306.biz.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import online.aquan.index12306.framework.starter.convention.exception.ServiceException;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.idempotent.annotation.Idempotent;
import online.aquan.index12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import online.aquan.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 支付结果回调购票消费者
 *  这条消息被两个消费者订阅了,一个是此服务,用于修改票的状态,另一个在订单模块,修改订单状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = TicketRocketMQConstant.PAY_GLOBAL_TOPIC_KEY,
        selectorExpression = TicketRocketMQConstant.PAY_RESULT_CALLBACK_TAG_KEY,
        consumerGroup = TicketRocketMQConstant.PAY_RESULT_CALLBACK_TICKET_CG_KEY
)
public class PayResultCallbackTicketConsumer implements RocketMQListener<MessageWrapper<PayResultCallbackTicketEvent>> {

    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final SeatMapper seatMapper;

    @Idempotent(
            uniqueKeyPrefix = "index12306-ticket:pay_result_callback:",
            key = "#message.getKeys()+'_'+#message.hashCode()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.MQ,
            keyTimeout = 7200L
    )
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void onMessage(MessageWrapper<PayResultCallbackTicketEvent> message) {
        Result<TicketOrderDetailRespDTO> ticketOrderDetailResult;
        try {
            //查询订单详情
            ticketOrderDetailResult = ticketOrderRemoteService.queryTicketOrderByOrderSn(message.getMessage().getOrderSn());
            if (!ticketOrderDetailResult.isSuccess() && Objects.isNull(ticketOrderDetailResult.getData())) {
                throw new ServiceException("支付结果回调查询订单失败");
            }
        } catch (Throwable ex) {
            log.error("支付结果回调查询订单失败", ex);
            throw ex;
        }
        TicketOrderDetailRespDTO ticketOrderDetail = ticketOrderDetailResult.getData();
        for (TicketOrderPassengerDetailRespDTO each : ticketOrderDetail.getPassengerDetails()) {
            LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                    .eq(SeatDO::getTrainId, ticketOrderDetail.getTrainId())
                    .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())
                    .eq(SeatDO::getSeatNumber, each.getSeatNumber())
                    .eq(SeatDO::getSeatType, each.getSeatType())
                    .eq(SeatDO::getStartStation, ticketOrderDetail.getDeparture())
                    .eq(SeatDO::getEndStation, ticketOrderDetail.getArrival());
            SeatDO updateSeatDO = new SeatDO();
            //更改座位为已售出状态
            updateSeatDO.setSeatStatus(SeatStatusEnum.SOLD.getCode());
            seatMapper.update(updateSeatDO, updateWrapper);
        }
    }
}
