package online.aquan.index12306.biz.orderservice.controller;

import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.orderservice.dto.req.TicketOrderCreateReqDTO;
import online.aquan.index12306.biz.orderservice.service.OrderService;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
