package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum FreezeTypeEnum {

    JUDICIAL(1, "司法冻结"),
    RISK_CONTROL(2, "风控冻结");

    private final Integer code;
    private final String desc;

    FreezeTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FreezeTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FreezeTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
