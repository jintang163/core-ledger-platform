package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum CurrencyEnum {

    CNY("CNY", "人民币"),
    USD("USD", "美元"),
    EUR("EUR", "欧元"),
    GBP("GBP", "英镑"),
    JPY("JPY", "日元");

    private final String code;
    private final String desc;

    CurrencyEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CurrencyEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (CurrencyEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
