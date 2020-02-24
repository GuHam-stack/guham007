package com.changgou.pay.service;

import java.util.Map;

/**
 * 微信支付接口
 * @author Steven
 * @description com.changgou.pay.service
 */
public interface WeixinPayService {
    /**
     * 生成微信支付二维码
     * @param out_trade_no 订单号
     * @param total_fee 金额(分)
     * @return
     */
    Map createNative(String out_trade_no, String total_fee);
}

