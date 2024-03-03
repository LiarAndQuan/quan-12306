package online.aquan.index12306.biz.ticketservice.remote;


import online.aquan.index12306.biz.ticketservice.remote.dto.PayInfoRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import online.aquan.index12306.framework.starter.convention.result.Result;

@FeignClient(value = "index12306-pay${unique-name:}-service", url = "${aggregation.remote-url:}")
public interface PayRemoteService {

    /**
     * 支付单详情查询
     */
    @GetMapping("/api/pay-service/pay/query")
    Result<PayInfoRespDTO> getPayInfo(@RequestParam(value = "orderSn") String orderSn);

}
