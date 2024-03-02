package online.aquan.index12306.biz.orderservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.aquan.index12306.biz.orderservice.dao.entity.OrderItemPassengerDO;
import online.aquan.index12306.biz.orderservice.dao.mapper.OrderItemPassengerMapper;
import online.aquan.index12306.biz.orderservice.service.OrderPassengerRelationService;
import org.springframework.stereotype.Service;

@Service
public class OrderPassengerRelationServiceImpl extends ServiceImpl<OrderItemPassengerMapper, OrderItemPassengerDO> implements OrderPassengerRelationService {
    
}
