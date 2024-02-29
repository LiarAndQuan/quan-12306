package online.aquan.index12306.biz.ticketservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.ticketservice.dao.entity.TrainStationDO;
import online.aquan.index12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import online.aquan.index12306.biz.ticketservice.dto.resp.TrainStationQueryRespDTO;
import online.aquan.index12306.biz.ticketservice.service.TrainStationService;
import online.aquan.index12306.framework.starter.common.toolkit.BeanUtil;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class TrainStationServiceImpl implements TrainStationService {
    
    private final TrainStationMapper trainStationMapper;

    @Override
    public List<TrainStationQueryRespDTO> listTrainStationQuery(String trainId) {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId);
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);
        return BeanUtil.convert(trainStationDOList, TrainStationQueryRespDTO.class);
    }
}
