package online.aquan.index12306.biz.ticketservice.service;

import online.aquan.index12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import online.aquan.index12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;

import java.util.List;

public interface RegionStationService {

    /**
     * 查询车站&城市站点集合信息
     *
     * @param requestParam 车站&站点查询参数
     * @return 车站&站点返回数据集合
     */
    List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO requestParam);

}
