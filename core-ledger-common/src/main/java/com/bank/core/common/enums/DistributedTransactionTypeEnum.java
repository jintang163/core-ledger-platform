package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum DistributedTransactionTypeEnum {

    TCC(1, "TCC模式"),
    SAGA(2, "Saga模式");

    private final Integer code;
    private final String desc;

    DistributedTransactionTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static DistributedTransactionTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DistributedTransactionTypeEnum e : DistributedTransactionTypeEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
