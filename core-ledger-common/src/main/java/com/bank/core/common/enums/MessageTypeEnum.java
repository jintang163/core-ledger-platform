package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum MessageTypeEnum {

    ACCOUNT_CHANGE(1, "ACCOUNT_CHANGE", "账户变动消息"),
    CLEARING_RESULT(2, "CLEARING_RESULT", "清算结果消息"),
    TRANSFER_SUCCESS(3, "TRANSFER_SUCCESS", "转账成功消息"),
    PAYMENT_SUCCESS(4, "PAYMENT_SUCCESS", "支付成功消息"),
    REFUND_SUCCESS(5, "REFUND_SUCCESS", "退款成功消息"),
    CALLBACK_NOTIFY(6, "CALLBACK_NOTIFY", "回调通知消息");

    private final Integer code;
    private final String name;
    private final String desc;

    MessageTypeEnum(Integer code, String name, String desc) {
        this.code = code;
        this.name = name;
        this.desc = desc;
    }

    public static MessageTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MessageTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }

    public static MessageTypeEnum getByName(String name) {
        if (name == null) {
            return null;
        }
        for (MessageTypeEnum e : values()) {
            if (e.getName().equals(name)) {
                return e;
            }
        }
        return null;
    }
}
