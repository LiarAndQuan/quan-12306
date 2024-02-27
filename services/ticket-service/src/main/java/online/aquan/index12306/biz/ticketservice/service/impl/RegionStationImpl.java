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

package online.aquan.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.common.enums.RegionStationQueryTypeEnum;
import online.aquan.index12306.biz.ticketservice.dao.entity.RegionDO;
import online.aquan.index12306.biz.ticketservice.dao.entity.StationDO;
import online.aquan.index12306.biz.ticketservice.dao.mapper.RegionMapper;
import online.aquan.index12306.biz.ticketservice.dao.mapper.StationMapper;
import online.aquan.index12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import online.aquan.index12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;
import online.aquan.index12306.biz.ticketservice.service.RegionStationService;
import online.aquan.index12306.framework.starter.common.enums.FlagEnum;
import online.aquan.index12306.framework.starter.common.toolkit.BeanUtil;
import online.aquan.index12306.framework.starter.convention.exception.ClientException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 地区以及车站接口实现层
 */
@Service
@RequiredArgsConstructor
public class RegionStationImpl implements RegionStationService {

    private final RegionMapper regionMapper;
    private final StationMapper stationMapper;

    @Override
    public List<RegionStationQueryRespDTO> listRegionStationQuery(RegionStationQueryReqDTO requestParam) {
        // TODO 请求缓存
        //如果传入了name,那么就根据name查询即可,或者是name的拼写
        if (StrUtil.isNotBlank(requestParam.getName())) {
            LambdaQueryWrapper<StationDO> queryWrapper = Wrappers.lambdaQuery(StationDO.class)
                    .likeRight(StationDO::getName, requestParam.getName())
                    .or()
                    .likeRight(StationDO::getSpell, requestParam.getName());
            List<StationDO> stationDOList = stationMapper.selectList(queryWrapper);
            return BeanUtil.convert(stationDOList, RegionStationQueryRespDTO.class);
        }
        // TODO 请求缓存
        LambdaQueryWrapper<RegionDO> queryWrapper;
        //如果没有传入name,那么就看查询类型
        switch (requestParam.getQueryType()) {
            //传入0表示查找热门城市
            case 0:
                queryWrapper = Wrappers.lambdaQuery(RegionDO.class)
                        .eq(RegionDO::getPopularFlag, FlagEnum.TRUE.code());
                break;
            //表示查找开头字母为A-E的城市,以此类推即可 
            case 1:
                queryWrapper = Wrappers.lambdaQuery(RegionDO.class)
                        .in(RegionDO::getInitial, RegionStationQueryTypeEnum.A_E.getSpells());
                break;
            case 2:
                queryWrapper = Wrappers.lambdaQuery(RegionDO.class)
                        .in(RegionDO::getInitial, RegionStationQueryTypeEnum.F_J.getSpells());
                break;
            case 3:
                queryWrapper = Wrappers.lambdaQuery(RegionDO.class)
                        .in(RegionDO::getInitial, RegionStationQueryTypeEnum.K_O.getSpells());
                break;
            case 4:
                queryWrapper = Wrappers.lambdaQuery(RegionDO.class)
                        .in(RegionDO::getInitial, RegionStationQueryTypeEnum.P_T.getSpells());
                break;
            case 5:
                queryWrapper = Wrappers.lambdaQuery(RegionDO.class)
                        .in(RegionDO::getInitial, RegionStationQueryTypeEnum.U_Z.getSpells());
                break;
            default:
                throw new ClientException("查询失败，请检查查询参数是否正确");
        }
        //使用经过筛选的wrapper
        List<RegionDO> regionDOList = regionMapper.selectList(queryWrapper);
        return BeanUtil.convert(regionDOList, RegionStationQueryRespDTO.class);
    }
}
