package online.aquan.index12306.biz.orderservice.service;

import online.aquan.index12306.biz.orderservice.dto.req.CancelTicketOrderReqDTO;
import online.aquan.index12306.biz.orderservice.dto.req.TicketOrderCreateReqDTO;
import online.aquan.index12306.biz.orderservice.dto.req.TicketOrderPageQueryReqDTO;
import online.aquan.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import online.aquan.index12306.framework.starter.convention.page.PageResponse;

public interface OrderService {

    /**
     * 创建火车票订单
     *
     * @param requestParam 商品订单入参
     * @return 订单号
     */
    String createTicketOrder(TicketOrderCreateReqDTO requestParam);

    /**
     * 关闭火车票订单
     *
     * @param requestParam 关闭火车票订单入参
     */
    boolean closeTickOrder(CancelTicketOrderReqDTO requestParam);

    /**
     * 取消火车票订单
     *
     * @param requestParam 取消火车票订单入参
     */
    boolean cancelTickOrder(CancelTicketOrderReqDTO requestParam);

    /**
     * 跟据订单号查询车票订单
     *
     * @param orderSn 订单号
     * @return 订单详情
     */
    TicketOrderDetailRespDTO queryTicketOrderByOrderSn(String orderSn);

    /**
     * 跟据用户名分页查询车票订单
     *
     * @param requestParam 跟据用户 ID 分页查询对象
     * @return 订单分页详情
     */
    PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam);

}
