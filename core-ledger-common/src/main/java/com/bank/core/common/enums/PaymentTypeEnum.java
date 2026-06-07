package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum PaymentTypeEnum {

    RECHARGE(1, "充值"),
    WITHDRAW(2, "提现"),
    TRANSFER(3, "转账"),
    BATCH_TRANSFER(4, "批量转账"),
    CROSS_BANK_TRANSFER(5, "跨行转账"),
    REFUND(6, "退款");

    private final Integer code;
    private final String desc;

    PaymentTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PaymentTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PaymentTypeEnum e : PaymentTypeEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
