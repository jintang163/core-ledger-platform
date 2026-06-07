package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum TransactionTypeEnum {

    TRANSFER(1, "转账"),
    DEPOSIT(2, "存款"),
    WITHDRAW(3, "取款"),
    FEE(4, "手续费"),
    INTEREST(5, "利息"),
    ADJUST(6, "调账"),
    REFUND(7, "退款"),
    CROSS_BANK_TRANSFER(8, "跨行转账");

    private final Integer code;
    private final String desc;

    TransactionTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TransactionTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TransactionTypeEnum e : TransactionTypeEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
