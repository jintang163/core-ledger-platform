package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum TransactionPhaseEnum {

    TRY(1, "Try阶段-预留资源"),
    CONFIRM(2, "Confirm阶段-确认执行"),
    CANCEL(3, "Cancel阶段-取消回滚"),
    FORWARD(4, "Saga正向操作"),
    COMPENSATE(5, "Saga补偿操作");

    private final Integer code;
    private final String desc;

    TransactionPhaseEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TransactionPhaseEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TransactionPhaseEnum e : TransactionPhaseEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
