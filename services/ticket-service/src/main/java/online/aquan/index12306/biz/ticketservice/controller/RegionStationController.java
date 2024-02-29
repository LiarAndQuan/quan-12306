package online.aquan.index12306.biz.ticketservice.controller;

import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import online.aquan.index12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;
import online.aquan.index12306.biz.ticketservice.service.RegionStationService;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class RegionStationController {
    
    private final RegionStationService regionStationService;

    /**
     * 根据车站名查询车站或者传入的type(首字母范围)查询城市站点集合信息
     */
    @GetMapping("/api/ticket-service/region-station/query")
    public Result<List<RegionStationQueryRespDTO>> listRegionStation(RegionStationQueryReqDTO requestParam) {
        return Results.success(regionStationService.listRegionStation(requestParam));
    }
}
