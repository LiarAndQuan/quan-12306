package online.aquan.index12306.biz.payservice.controller;


import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.payservice.dto.PayInfoRespDTO;
import online.aquan.index12306.biz.payservice.service.PayService;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    /**
     * 根据订单号查询支付单详情
     */
    @GetMapping("/api/pay-service/pay/query/order-sn")
    public Result<PayInfoRespDTO> getPayInfoByOrderSn(@RequestParam(value = "orderSn") String orderSn) {
        return Results.success(payService.getPayInfoByOrderSn(orderSn));
    }
}
