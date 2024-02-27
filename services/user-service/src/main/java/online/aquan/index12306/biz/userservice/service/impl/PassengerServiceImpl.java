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

package online.aquan.index12306.biz.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.userservice.dao.entity.PassengerDO;
import online.aquan.index12306.biz.userservice.dao.mapper.PassengerMapper;
import online.aquan.index12306.biz.userservice.dto.req.PassengerReqDTO;
import online.aquan.index12306.biz.userservice.dto.resp.PassengerRespDTO;
import online.aquan.index12306.biz.userservice.service.PassengerService;
import online.aquan.index12306.framework.starter.common.toolkit.BeanUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 乘车人接口实现层
 */
@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerMapper passengerMapper;

    @Override
    public List<PassengerRespDTO> listPassengerQuery(String username) {
        LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                .eq(PassengerDO::getUsername, username);
        List<PassengerDO> passengerDOList = passengerMapper.selectList(queryWrapper);
        return BeanUtil.convert(passengerDOList, PassengerRespDTO.class);
    }

    @Override
    public void savePassenger(PassengerReqDTO requestParam) {
        PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
        passengerDO.setCreateDate(new Date());
        passengerDO.setVerifyStatus(0);
        // TODO 用户名和当前用户比对
        passengerMapper.insert(passengerDO);
    }

    @Override
    public void updatePassenger(PassengerReqDTO requestParam) {
        PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
        LambdaUpdateWrapper<PassengerDO> updateWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                // TODO 用户名和当前用户比对
                .eq(PassengerDO::getUsername, requestParam.getUsername())
                .eq(PassengerDO::getId, requestParam.getId());
        passengerMapper.update(passengerDO, updateWrapper);
    }
}
