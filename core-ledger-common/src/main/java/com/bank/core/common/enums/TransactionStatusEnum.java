package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum TransactionStatusEnum {

    PENDING(0, "待处理"),
    SUCCESS(1, "成功"),
    FAILED(2, "失败"),
    REVERSED(3, "已冲正");

    private final Integer code;
    private final String desc;

    TransactionStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TransactionStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TransactionStatusEnum e : TransactionStatusEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
