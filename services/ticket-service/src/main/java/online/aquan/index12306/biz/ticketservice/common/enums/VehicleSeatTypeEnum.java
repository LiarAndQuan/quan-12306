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

package online.aquan.index12306.biz.ticketservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

/**
 * 交通工具座位类型
 */
@RequiredArgsConstructor
public enum VehicleSeatTypeEnum {

    /**
     * 商务座
     */
    BUSINESS_CLASS(0, "BUSINESS_CLASS"),

    /**
     * 一等座
     */
    FIRST_CLASS(1, "FIRST_CLASS"),

    /**
     * 二等座
     */
    SECOND_CLASS(2, "SECOND_CLASS");

    @Getter
    private final Integer code;

    @Getter
    private final String name;

    /**
     * 根据编码查找名称
     */
    public static String findNameByCode(Integer code) {
        return Arrays.stream(VehicleSeatTypeEnum.values())
                .filter(each -> Objects.equals(each.getCode(), code))
                .findFirst()
                .map(VehicleSeatTypeEnum::getName)
                .orElse(null);
    }
}
