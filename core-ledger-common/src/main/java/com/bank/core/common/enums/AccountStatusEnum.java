package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum AccountStatusEnum {

    NORMAL(1, "正常"),
    FROZEN(2, "冻结"),
    CLOSED(3, "已销户");

    private final Integer code;
    private final String desc;

    AccountStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AccountStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AccountStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
