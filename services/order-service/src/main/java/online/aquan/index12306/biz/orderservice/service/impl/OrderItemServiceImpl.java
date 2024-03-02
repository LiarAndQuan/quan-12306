package online.aquan.index12306.biz.orderservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.orderservice.dao.entity.OrderItemDO;
import online.aquan.index12306.biz.orderservice.dao.mapper.OrderItemMapper;
import online.aquan.index12306.biz.orderservice.service.OrderItemService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItemDO> implements OrderItemService {
}
