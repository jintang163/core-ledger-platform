package com.bank.core.common.enums;

import lombok.Getter;

/**
 * 渠道编码枚举
 * 定义系统支持的所有外部支付渠道
 */
@Getter
public enum ChannelCodeEnum {

    /** 支付宝 */
    ALIPAY("ALIPAY", "支付宝", "CLEARING_ALIPAY"),
    /** 微信支付 */
    WECHAT("WECHAT", "微信支付", "CLEARING_WECHAT"),
    /** 银联支付 */
    UNIONPAY("UNIONPAY", "银联支付", "CLEARING_UNIONPAY"),
    /** 银行转账 */
    BANK_TRANSFER("BANK_TRANSFER", "银行转账", "CLEARING_BANK"),
    /** 模拟渠道（测试用） */
    MOCK("MOCK", "模拟渠道", "CLEARING_MOCK");

    /** 渠道编码 */
    private final String code;
    /** 渠道名称 */
    private final String name;
    /** 关联的清算账户ID */
    private final String clearingAccountId;

    ChannelCodeEnum(String code, String name, String clearingAccountId) {
        this.code = code;
        this.name = name;
        this.clearingAccountId = clearingAccountId;
    }

    /**
     * 根据编码获取渠道枚举
     * @param code 渠道编码
     * @return 渠道枚举，找不到返回null
     */
    public static ChannelCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ChannelCodeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据渠道编码获取清算账户ID
     * @param channelCode 渠道编码
     * @return 清算账户ID，找不到返回null
     */
    public static String getClearingAccountId(String channelCode) {
        ChannelCodeEnum channel = getByCode(channelCode);
        return channel != null ? channel.getClearingAccountId() : null;
    }
}
