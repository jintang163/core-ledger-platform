package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum AccountTypeEnum {

    PERSONAL(1, "个人账户"),
    ENTERPRISE(2, "企业账户");

    private final Integer code;
    private final String desc;

    AccountTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AccountTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AccountTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
