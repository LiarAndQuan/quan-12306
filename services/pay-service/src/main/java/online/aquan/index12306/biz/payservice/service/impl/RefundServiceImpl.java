package online.aquan.index12306.biz.payservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.aquan.index12306.biz.payservice.common.enums.TradeStatusEnum;
import online.aquan.index12306.biz.payservice.convert.RefundRequestConvert;
import online.aquan.index12306.biz.payservice.dao.entity.PayDO;
import online.aquan.index12306.biz.payservice.dao.entity.RefundDO;
import online.aquan.index12306.biz.payservice.dao.mapper.PayMapper;
import online.aquan.index12306.biz.payservice.dao.mapper.RefundMapper;
import online.aquan.index12306.biz.payservice.dto.RefundCommand;
import online.aquan.index12306.biz.payservice.dto.RefundCreateDTO;
import online.aquan.index12306.biz.payservice.dto.RefundReqDTO;
import online.aquan.index12306.biz.payservice.dto.RefundRespDTO;
import online.aquan.index12306.biz.payservice.dto.base.RefundRequest;
import online.aquan.index12306.biz.payservice.dto.base.RefundResponse;
import online.aquan.index12306.biz.payservice.mq.event.RefundResultCallbackOrderEvent;
import online.aquan.index12306.biz.payservice.mq.produce.RefundResultCallbackOrderSendProduce;
import online.aquan.index12306.biz.payservice.remote.TicketOrderRemoteService;
import online.aquan.index12306.biz.payservice.remote.dto.TicketOrderDetailRespDTO;
import online.aquan.index12306.biz.payservice.service.RefundService;
import online.aquan.index12306.framework.starter.common.toolkit.BeanUtil;
import online.aquan.index12306.framework.starter.convention.exception.ServiceException;
import online.aquan.index12306.framework.starter.convention.result.Result;
import online.aquan.index12306.framework.starter.designpattern.strategy.AbstractStrategyChoose;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;


@RequiredArgsConstructor
@Service
@Slf4j
public class RefundServiceImpl implements RefundService {

    private final PayMapper payMapper;
    private final RefundMapper refundMapper;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final AbstractStrategyChoose abstractStrategyChoose;
    private final RefundResultCallbackOrderSendProduce refundResultCallbackOrderSendProduce;

    @Override
    @Transactional
    public RefundRespDTO commonRefund(RefundReqDTO requestParam) {
        RefundRespDTO refundRespDTO = null;
        //查询一下这个支付单是否存在
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        PayDO payDO = payMapper.selectOne(queryWrapper);
        if (Objects.isNull(payDO)) {
            log.error("支付单不存在，orderSn：{}", requestParam.getOrderSn());
            throw new ServiceException("支付单不存在");
        }
        //之前订单里面的总金额减掉退款的金额
        payDO.setPayAmount(payDO.getTotalAmount() - requestParam.getRefundAmount());
        //创建退款单,里面有需要退款车票的集合
        RefundCreateDTO refundCreateDTO = BeanUtil.convert(requestParam, RefundCreateDTO.class);
        refundCreateDTO.setPaySn(payDO.getPaySn());
        //远程调用车票服务查询出对应车票,用于构建退款单的信息
        createRefund(refundCreateDTO);
        //策略模式：通过策略模式封装退款渠道和退款场景，用户退款时动态选择对应的退款组件
        RefundCommand refundCommand = BeanUtil.convert(payDO, RefundCommand.class);
        refundCommand.setPayAmount(new BigDecimal(requestParam.getRefundAmount()));
        RefundRequest refundRequest = RefundRequestConvert.command2RefundRequest(refundCommand);
        RefundResponse result = abstractStrategyChoose.chooseAndExecuteResp(refundRequest.buildMark(), refundRequest);
        //修改支付单里面的金额和支付状态
        payDO.setStatus(result.getStatus());
        LambdaUpdateWrapper<PayDO> updateWrapper = Wrappers.lambdaUpdate(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        int updateResult = payMapper.update(payDO, updateWrapper);
        if (updateResult <= 0) {
            log.error("修改支付单退款结果失败，支付单信息：{}", JSON.toJSONString(payDO));
            throw new ServiceException("修改支付单退款结果失败");
        }
        //修改退款表中的退款状态和交易凭证
        LambdaUpdateWrapper<RefundDO> refundUpdateWrapper = Wrappers.lambdaUpdate(RefundDO.class)
                .eq(RefundDO::getOrderSn, requestParam.getOrderSn());
        RefundDO refundDO = new RefundDO();
        refundDO.setTradeNo(result.getTradeNo());
        refundDO.setStatus(result.getStatus());
        int refundUpdateResult = refundMapper.update(refundDO, refundUpdateWrapper);
        if (refundUpdateResult <= 0) {
            log.error("修改退款单退款结果失败，退款单信息：{}", JSON.toJSONString(refundDO));
            throw new ServiceException("修改退款单退款结果失败");
        }
        // 退款成功，回调订单服务告知退款结果，修改订单流转状态
        if (Objects.equals(result.getStatus(), TradeStatusEnum.TRADE_CLOSED.tradeCode())) {
            RefundResultCallbackOrderEvent refundResultCallbackOrderEvent = RefundResultCallbackOrderEvent.builder()
                    .orderSn(requestParam.getOrderSn())
                    .refundTypeEnum(requestParam.getRefundTypeEnum())
                    .partialRefundTicketDetailList(requestParam.getRefundDetailReqDTOList())
                    .build();
            refundResultCallbackOrderSendProduce.sendMessage(refundResultCallbackOrderEvent);
        }
        //TODO 暂时返回空实体
        return refundRespDTO;
    }

    private void createRefund(RefundCreateDTO requestParam) {
        //根据订单号查询出车票订单的基本信息,比如起始站点结束站点之类的
        Result<TicketOrderDetailRespDTO> queryTicketResult = ticketOrderRemoteService.queryTicketOrderByOrderSn(requestParam.getOrderSn());
        if (!queryTicketResult.isSuccess() && Objects.isNull(queryTicketResult.getData())) {
            throw new ServiceException("车票订单不存在");
        }
        TicketOrderDetailRespDTO orderDetailRespDTO = queryTicketResult.getData();
        //遍历需要退票的车票
        requestParam.getRefundDetailReqDTOList().forEach(each -> {
            RefundDO refundDO = new RefundDO();
            refundDO.setPaySn(requestParam.getPaySn());
            refundDO.setOrderSn(requestParam.getOrderSn());
            refundDO.setTrainId(orderDetailRespDTO.getTrainId());
            refundDO.setTrainNumber(orderDetailRespDTO.getTrainNumber());
            refundDO.setDeparture(orderDetailRespDTO.getDeparture());
            refundDO.setArrival(orderDetailRespDTO.getArrival());
            refundDO.setDepartureTime(orderDetailRespDTO.getDepartureTime());
            refundDO.setArrivalTime(orderDetailRespDTO.getArrivalTime());
            refundDO.setRidingDate(orderDetailRespDTO.getRidingDate());
            refundDO.setSeatType(each.getSeatType());
            refundDO.setIdType(each.getIdType());
            refundDO.setIdCard(each.getIdCard());
            refundDO.setRealName(each.getRealName());
            refundDO.setRefundTime(new Date());
            refundDO.setAmount(each.getAmount());
            refundDO.setUserId(each.getUserId());
            refundDO.setUsername(each.getUsername());
            refundMapper.insert(refundDO);
        });
    }

}
