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

package online.aquan.index12306.biz.ticketservice.common.constant;

/**
 * RocketMQ 购票服务常量类
 *
 */
public final class TicketRocketMQConstant {
    
    /**
     * 购票服务创建订单后延时关闭业务消费者组 Key
     */
    public static final String TICKET_DELAY_CLOSE_CG_KEY = "index12306_ticket-service_delay-close-order_cg${unique-name:}";

    /**
     * 订单服务相关业务 Topic Key
     */
    public static final String ORDER_DELAY_CLOSE_TOPIC_KEY = "index12306_order-service_delay-close-order_topic${unique-name:}";

    /**
     * 购票服务创建订单后延时关闭业务 Tag Key
     */
    public static final String ORDER_DELAY_CLOSE_TAG_KEY = "index12306_order-service_delay-close-order_tag${unique-name:}";


}
