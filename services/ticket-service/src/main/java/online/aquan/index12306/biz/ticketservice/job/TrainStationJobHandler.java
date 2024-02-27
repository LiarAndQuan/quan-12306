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

package online.aquan.index12306.biz.ticketservice.job;

import com.alibaba.fastjson2.JSON;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.dao.entity.TrainDO;
import online.aquan.index12306.biz.ticketservice.dto.resp.TrainStationQueryRespDTO;
import online.aquan.index12306.biz.ticketservice.job.base.AbstractTrainStationJobHandlerTemplate;
import online.aquan.index12306.biz.ticketservice.service.TrainStationService;
import online.aquan.index12306.framework.starter.cache.DistributedCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static online.aquan.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_STOPOVER_DETAIL;

/**
 * 列车路线信息定时任务
 */
@RestController
@RequiredArgsConstructor
public class TrainStationJobHandler extends AbstractTrainStationJobHandlerTemplate {

    private final TrainStationService trainStationService;
    private final DistributedCache distributedCache;

    @XxlJob(value = "trainStationJobHandler")
    @GetMapping("/api/ticket-service/train-station/job/cache-init/execute")
    @Override
    public void execute() {
        super.execute();
    }

    @Override
    protected void actualExecute(List<TrainDO> trainDOPageRecords) {
        for (TrainDO each : trainDOPageRecords) {
            List<TrainStationQueryRespDTO> listedTrainStationQuery = trainStationService.listTrainStationQuery(each.getId().toString());
            distributedCache.put(TRAIN_STATION_STOPOVER_DETAIL + each.getId(), JSON.toJSONString(listedTrainStationQuery));
        }
    }
}
