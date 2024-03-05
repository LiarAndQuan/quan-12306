package online.aquan.index12306.biz.ticketservice.remote;


import online.aquan.index12306.biz.ticketservice.remote.dto.PayInfoRespDTO;
import online.aquan.index12306.biz.ticketservice.remote.dto.RefundReqDTO;
import online.aquan.index12306.biz.ticketservice.remote.dto.RefundRespDTO;
import online.aquan.index12306.framework.starter.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "index12306-pay${unique-name:}-service", url = "${aggregation.remote-url:}")
public interface PayRemoteService {

    /**
     * 支付单详情查询
     */
    @GetMapping("/api/pay-service/pay/query")
    Result<PayInfoRespDTO> getPayInfo(@RequestParam(value = "orderSn") String orderSn);

    /**
     * 公共退款接口
     */
    @PostMapping("/api/pay-service/common/refund")
    Result<RefundRespDTO> commonRefund(@RequestBody RefundReqDTO requestParam);
}
