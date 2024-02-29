package online.aquan.index12306.biz.ticketservice.service;

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

}
