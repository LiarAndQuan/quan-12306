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

package online.aquan.index12306.biz.ticketservice.controller;

import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import online.aquan.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import online.aquan.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import online.aquan.index12306.biz.ticketservice.dto.resp.TicketPurchaseRespDTO;
import online.aquan.index12306.biz.ticketservice.service.TicketService;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.idempotent.annotation.Idempotent;
import online.aquan.index12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import online.aquan.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;
import online.aquan.index12306.framework.starter.log.annotation.ILog;
import online.aquan.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * 根据条件查询车票
     */
    @GetMapping("/api/ticket-service/ticket/query")
    public Result<TicketPageQueryRespDTO> pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        return Results.success(ticketService.pageListTicketQueryV1(requestParam));
    }

    /**
     * 购买车票
     */
    @ILog
    @Idempotent(uniqueKeyPrefix = "index12306-ticket:lock_purchase-tickets:", key = "T(online. aquan.index12306.framework.starter.bases.ApplicationContextHolder).getBean('environment').getProperty('unique-name', '')"
            + "+'_'+"
            + "T(online.aquan.index12306.frameworks.starter.user.core.UserContext).getUsername()", message = "正在执行下单流程，请稍后...", scene = IdempotentSceneEnum.RESTAPI, type = IdempotentTypeEnum.SPEL)
    @PostMapping("/api/ticket-service/ticket/purchase")
    public Result<TicketPurchaseRespDTO> purchaseTickets(@RequestBody PurchaseTicketReqDTO requestParam) {
        return Results.success(ticketService.purchaseTicketsV1(requestParam));
    }

}
