package online.aquan.index12306.biz.payservice.service;

import online.aquan.index12306.biz.payservice.dto.PayCallbackReqDTO;
import online.aquan.index12306.biz.payservice.dto.PayInfoRespDTO;
import online.aquan.index12306.biz.payservice.dto.PayRespDTO;
import online.aquan.index12306.biz.payservice.dto.base.PayRequest;

public interface PayService {

    /**
     * 跟据订单号查询支付单详情
     *
     * @param orderSn 订单号
     * @return 支付单详情
     */
    PayInfoRespDTO getPayInfoByOrderSn(String orderSn);


    /**
     * 跟据支付流水号查询支付单详情
     *
     * @param paySn 支付单流水号
     * @return 支付单详情
     */
    PayInfoRespDTO getPayInfoByPaySn(String paySn);

    /**
     * 创建支付单
     *
     * @param requestParam 创建支付单实体
     * @return 支付返回详情
     */
    PayRespDTO commonPay(PayRequest requestParam);

    /**
     * 支付单回调
     *
     * @param requestParam 回调支付单实体
     */
    void callbackPay(PayCallbackReqDTO requestParam);

}
