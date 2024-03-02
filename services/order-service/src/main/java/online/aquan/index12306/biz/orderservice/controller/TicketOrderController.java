package online.aquan.index12306.biz.orderservice.controller;

import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.orderservice.dto.req.CancelTicketOrderReqDTO;
import online.aquan.index12306.biz.orderservice.dto.req.TicketOrderCreateReqDTO;
import online.aquan.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import online.aquan.index12306.biz.orderservice.service.OrderService;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TicketOrderController {
    
    private OrderService orderService;
    
    /**
     * 车票订单创建
     */
    @PostMapping("/api/order-service/order/ticket/create")
    public Result<String> createTicketOrder(@RequestBody TicketOrderCreateReqDTO requestParam) {
        return Results.success(orderService.createTicketOrder(requestParam));
    }

    /**
     * 车票订单关闭
     */
    @PostMapping("/api/order-service/order/ticket/close")
    public Result<Boolean> closeTickOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        return Results.success(orderService.closeTickOrder(requestParam));
    }
    /**
     * 车票订单取消
     */
    @PostMapping("/api/order-service/order/ticket/cancel")
    public Result<Boolean> cancelTickOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        return Results.success(orderService.cancelTickOrder(requestParam));
    }

    /**
     * 根据订单号查询车票订单
     */
    @GetMapping("/api/order-service/order/ticket/query")
    public Result<TicketOrderDetailRespDTO> queryTicketOrderByOrderSn(@RequestParam(value = "orderSn") String orderSn) {
        return Results.success(orderService.queryTicketOrderByOrderSn(orderSn));
    }
}
