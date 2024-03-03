package online.aquan.index12306.biz.payservice.service;

import online.aquan.index12306.biz.payservice.dto.PayInfoRespDTO;

public interface PayService {

    /**
     * 跟据订单号查询支付单详情
     *
     * @param orderSn 订单号
     * @return 支付单详情
     */
    PayInfoRespDTO getPayInfoByOrderSn(String orderSn);

}
