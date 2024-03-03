package online.aquan.index12306.biz.payservice.controller;


import lombok.RequiredArgsConstructor;
import online.aquan.index12306.biz.payservice.convert.PayRequestConvert;
import online.aquan.index12306.biz.payservice.dto.PayCommand;
import online.aquan.index12306.biz.payservice.dto.PayInfoRespDTO;
import online.aquan.index12306.biz.payservice.dto.PayRespDTO;
import online.aquan.index12306.biz.payservice.dto.base.PayRequest;
import online.aquan.index12306.biz.payservice.service.PayService;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.*;


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

    /**
     * 跟据支付流水号查询支付单详情
     */
    @GetMapping("/api/pay-service/pay/query/pay-sn")
    public Result<PayInfoRespDTO> getPayInfoByPaySn(@RequestParam(value = "paySn") String paySn) {
        return Results.success(payService.getPayInfoByPaySn(paySn));
    }

    /**
     * 公共支付接口
     * 对接常用支付方式，比如：支付宝、微信以及银行卡等
     * command里面是支付渠道,类型,金额,订单号等信息
     */
    @PostMapping("/api/pay-service/pay/create")
    public Result<PayRespDTO> pay(@RequestBody PayCommand requestParam) {
        //转化成ali的pay request
        PayRequest payRequest = PayRequestConvert.command2PayRequest(requestParam);
        PayRespDTO result = payService.commonPay(payRequest);
        return Results.success(result);
    }
    
    
}
