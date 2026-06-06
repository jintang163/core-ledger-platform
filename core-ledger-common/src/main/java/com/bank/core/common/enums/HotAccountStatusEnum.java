package com.bank.core.common.enums;

import lombok.Getter;

/**
 * 热点账户状态枚举
 * 用于标记账户是否为热点账户，以及是否启用分片等特性
 */
@Getter
public enum HotAccountStatusEnum {

    NORMAL(0, "普通账户"),

    HOT_PENDING(1, "待标记热点账户"),

    HOT_SHARDING(2, "已分片热点账户"),

    HOT_BUFFER(3, "已启用缓冲记账");

    private final Integer code;
    private final String desc;

    HotAccountStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static HotAccountStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (HotAccountStatusEnum e : HotAccountStatusEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
