package online.aquan.index12306.biz.ticketservice.service;

import online.aquan.index12306.biz.ticketservice.dto.domain.RouteDTO;
import online.aquan.index12306.biz.ticketservice.dto.resp.TrainStationQueryRespDTO;

import java.util.List;

public interface TrainStationService {

    /**
     * 根据列车 ID 查询站点信息
     *
     * @param trainId 列车 ID
     * @return 列车经停站信息
     */
    List<TrainStationQueryRespDTO> listTrainStationQuery(String trainId);

    /**
     * 计算列车站点路线关系
     * 获取开始站点和目的站点及中间站点信息
     *
     * @param trainId   列车 ID
     * @param departure 出发站
     * @param arrival   到达站
     * @return 列车站点路线关系信息
     */
    List<RouteDTO> listTrainStationRoute(String trainId, String departure, String arrival);

}
