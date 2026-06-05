package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum DebitCreditEnum {

    DEBIT(1, "借"),
    CREDIT(2, "贷");

    private final Integer code;
    private final String desc;

    DebitCreditEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static DebitCreditEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DebitCreditEnum e : DebitCreditEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
