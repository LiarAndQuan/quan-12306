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

package online.aquan.index12306.biz.ticketservice.dto.domain;

import lombok.Data;

/**
 * 动车实体
 */
@Data
public class BulletTrainDTO {

    /**
     * 商务座数量
     */
    private Integer businessClassQuantity;

    /**
     * 商务座候选标识
     */
    private Boolean businessClassCandidate;

    /**
     * 商务座价格
     */
    private Integer businessClassPrice;

    /**
     * 一等座数量
     */
    private Integer firstClassQuantity;

    /**
     * 一等座候选标识
     */
    private Boolean firstClassCandidate;

    /**
     * 一等座价格
     */
    private Integer firstClassPrice;

    /**
     * 二等座数量
     */
    private Integer secondClassQuantity;

    /**
     * 二等座候选标识
     */
    private Boolean secondClassCandidate;

    /**
     * 二等座价格
     */
    private Integer secondClassPrice;
}
