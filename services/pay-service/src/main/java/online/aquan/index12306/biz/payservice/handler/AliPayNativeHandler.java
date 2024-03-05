/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package online.aquan.index12306.biz.payservice.handler;

import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import online.aquan.index12306.biz.payservice.common.enums.PayChannelEnum;
import online.aquan.index12306.biz.payservice.common.enums.PayTradeTypeEnum;
import online.aquan.index12306.biz.payservice.config.AliPayProperties;
import online.aquan.index12306.biz.payservice.dto.base.AliPayRequest;
import online.aquan.index12306.biz.payservice.dto.base.PayRequest;
import online.aquan.index12306.biz.payservice.dto.base.PayResponse;
import online.aquan.index12306.biz.payservice.handler.base.AbstractPayHandler;
import online.aquan.index12306.framework.starter.common.toolkit.BeanUtil;
import online.aquan.index12306.framework.starter.convention.exception.ServiceException;
import online.aquan.index12306.framework.starter.designpattern.strategy.AbstractExecuteStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;


/**
 * 阿里支付组件,官网示例地址:<a href="https://opendocs.alipay.com/open/59da99d0_alipay.trade.page.pay?scene=22&pathHash=e26b497f#%E5%85%AC%E5%85%B1%E8%AF%B7%E6%B1%82%E5%8F%82%E6%95%B0">...</a>
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliPayNativeHandler extends AbstractPayHandler implements AbstractExecuteStrategy<PayRequest, PayResponse> {

    private final AliPayProperties aliPayProperties;
    
    @SneakyThrows(value = AlipayApiException.class)
    @Override
    @Retryable(value = ServiceException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 1.5))
    public PayResponse pay(PayRequest payRequest) {
        //从入参中获取到阿里支付入参
        AliPayRequest aliPayRequest = payRequest.getAliPayRequest();
        //获取AlipayConfig,里面包括了私钥,appid等
        AlipayConfig alipayConfig = BeanUtil.convert(aliPayProperties, AlipayConfig.class);
        //获取AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(alipayConfig);
        //设置四个必选属性:订单号,金额,主题,销售产品码
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(aliPayRequest.getOrderSn());
        model.setTotalAmount(aliPayRequest.getTotalAmount().toString());
        model.setSubject(aliPayRequest.getSubject());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        //获取真正的请求对象
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //设置回调地址
        request.setNotifyUrl(aliPayProperties.getNotifyUrl());
        request.setBizModel(model);
        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            log.info("发起支付宝支付，订单号：{}，子订单号：{}，订单请求号：{}，订单金额：{} \n调用支付返回：\n\n{}\n",
                    aliPayRequest.getOrderSn(),
                    aliPayRequest.getOutOrderSn(),
                    aliPayRequest.getOrderRequestId(),
                    aliPayRequest.getTotalAmount(),
                    JSONObject.toJSONString(response));
            if (!response.isSuccess()) {
                throw new ServiceException("调用支付宝发起支付异常");
            }
            return new PayResponse(StrUtil.replace(StrUtil.replace(response.getBody(), "\"", "'"), "\n", ""));
        } catch (AlipayApiException ex) {
            throw new ServiceException("调用支付宝支付异常");
        }
    }

    @Override
    public String mark() {
        return StrBuilder.create()
                .append(PayChannelEnum.ALI_PAY.name())
                .append("_")
                .append(PayTradeTypeEnum.NATIVE.name())
                .toString();
    }

    @Override
    public PayResponse executeResp(PayRequest requestParam) {
        return pay(requestParam);
    }
}
