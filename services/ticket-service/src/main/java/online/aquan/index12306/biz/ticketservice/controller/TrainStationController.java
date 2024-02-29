package online.aquan.index12306.biz.ticketservice.controller;

import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.dto.resp.TrainStationQueryRespDTO;
import online.aquan.index12306.biz.ticketservice.service.TrainStationService;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TrainStationController {

    private final TrainStationService trainStationService;

    /**
     * 根据列车 ID 查询站点信息,返回的是所有站点的名字和顺序
     */
    @GetMapping("/api/ticket-service/train-station/query")
    public Result<List<TrainStationQueryRespDTO>> listTrainStationQuery(String trainId) {
        return Results.success(trainStationService.listTrainStationQuery(trainId));
    }
}
