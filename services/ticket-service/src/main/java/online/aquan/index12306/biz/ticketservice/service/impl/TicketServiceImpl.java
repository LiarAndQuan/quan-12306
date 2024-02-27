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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.dao.entity.TrainDO;
import online.aquan.index12306.biz.ticketservice.dao.entity.TrainStationPriceDO;
import online.aquan.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import online.aquan.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import online.aquan.index12306.biz.ticketservice.dao.mapper.TrainStationPriceMapper;
import online.aquan.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import online.aquan.index12306.biz.ticketservice.dto.domain.BulletTrainDTO;
import online.aquan.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import online.aquan.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import online.aquan.index12306.biz.ticketservice.service.TicketService;
import online.aquan.index12306.biz.ticketservice.toolkit.DateUtil;
import online.aquan.index12306.framework.starter.cache.DistributedCache;
import online.aquan.index12306.framework.starter.convention.page.PageResponse;
import online.aquan.index12306.framework.starter.database.toolkit.PageUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static online.aquan.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;


/**
 * 车票接口实现
 */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TrainMapper trainMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final DistributedCache distributedCache;

    @Override
    public PageResponse<TicketPageQueryRespDTO> pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        // TODO 责任链模式 验证城市名称是否存在、不存在加载缓存等等
        //先根据列车站点之间的关系查询所有的列车
        LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                .eq(TrainStationRelationDO::getStartRegion, requestParam.getFromStation())
                .eq(TrainStationRelationDO::getEndRegion, requestParam.getToStation());
        //将自定义的分页对象转化为Page对象,分页查询出所有的列车
        IPage<TrainStationRelationDO> trainStationRelationPage = trainStationRelationMapper.selectPage(PageUtil.convert(requestParam), queryWrapper);
        //再将Page对象转化成自定义的分页对象
        return PageUtil.convert(trainStationRelationPage, each -> {
            //对每一个对象,查询出列车的详细信息
            LambdaQueryWrapper<TrainDO> trainQueryWrapper = Wrappers.lambdaQuery(TrainDO.class).eq(TrainDO::getId, each.getTrainId());
            TrainDO trainDO = trainMapper.selectOne(trainQueryWrapper);
            TicketPageQueryRespDTO result = new TicketPageQueryRespDTO();
            //在结果中设置基本的参数
            result.setTrainNumber(trainDO.getTrainNumber());
            result.setDepartureTime(each.getDepartureTime());
            result.setArrivalTime(each.getArrivalTime());
            result.setDuration(DateUtil.calculateHourDifference(each.getDepartureTime(), each.getArrivalTime()));
            result.setDeparture(each.getDeparture());
            result.setArrival(each.getArrival());
            result.setDepartureFlag(each.getDepartureFlag());
            result.setArrivalFlag(each.getArrivalFlag());
            //如果是高铁,在结果中把座位等级,数量以及价格加入
            if (Objects.equals(trainDO.getTrainType(), 0)) {
                BulletTrainDTO bulletTrainDTO = new BulletTrainDTO();
                LambdaQueryWrapper<TrainStationPriceDO> trainStationPriceQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                        .eq(TrainStationPriceDO::getDeparture, each.getDeparture())
                        .eq(TrainStationPriceDO::getArrival, each.getArrival())
                        .eq(TrainStationPriceDO::getTrainId, each.getTrainId());
                //这里得到三种票的信息
                List<TrainStationPriceDO> trainStationPriceDOList = trainStationPriceMapper.selectList(trainStationPriceQueryWrapper);
                //获取StringRedisTemplate的实例
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                trainStationPriceDOList.forEach(item -> {
                    //得到车号+起点+终点的字符串
                    String keySuffix = StrUtil.join("_", each.getTrainId(), item.getDeparture(), item.getArrival());
                    switch (item.getSeatType()) {
                        case 0:
                            //获取商务座座位类型的余票
                            String businessClassQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "0");
                            bulletTrainDTO.setBusinessClassQuantity(Integer.parseInt(businessClassQuantity));
                            bulletTrainDTO.setBusinessClassPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            bulletTrainDTO.setBusinessClassCandidate(false);
                            break;
                        case 1:
                            String firstClassQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "1");
                            bulletTrainDTO.setFirstClassQuantity(Integer.parseInt(firstClassQuantity));
                            bulletTrainDTO.setFirstClassPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            bulletTrainDTO.setFirstClassCandidate(false);
                            break;
                        case 2:
                            String secondClassQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "2");
                            bulletTrainDTO.setSecondClassQuantity(Integer.parseInt(secondClassQuantity));
                            bulletTrainDTO.setSecondClassPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            bulletTrainDTO.setSecondClassCandidate(false);
                            break;
                        default:
                            break;
                    }
                });
                //添加上对应的信息
                result.setBulletTrain(bulletTrainDTO);
            }
            return result;
        });
    }
}
