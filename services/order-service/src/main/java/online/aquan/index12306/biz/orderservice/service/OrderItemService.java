package online.aquan.index12306.biz.orderservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import online.aquan.index12306.biz.orderservice.dao.entity.OrderItemDO;
import online.aquan.index12306.biz.orderservice.dto.domain.OrderItemStatusReversalDTO;
import online.aquan.index12306.biz.orderservice.dto.req.TicketOrderItemQueryReqDTO;
import online.aquan.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;

import java.util.List;

public interface OrderItemService extends IService<OrderItemDO> {

    /**
     * 根据子订单记录id查询车票子订单详情
     *
     * @param requestParam 请求参数
     */
    List<TicketOrderPassengerDetailRespDTO> queryTicketItemOrderById(TicketOrderItemQueryReqDTO requestParam);

    /**
     * 子订单状态反转
     *
     * @param requestParam 请求参数
     */
    void orderItemStatusReversal(OrderItemStatusReversalDTO requestParam);

}

