package com.bank.core.common.enums;

import lombok.Getter;

/**
 * 缓冲记账状态枚举
 * 用于缓冲记账流水的状态管理
 */
@Getter
public enum BufferStatusEnum {

    PENDING(0, "待处理"),

    PROCESSING(1, "处理中"),

    SUCCESS(2, "处理成功"),

    FAILED(3, "处理失败"),

    MERGED(4, "已归并");

    private final Integer code;
    private final String desc;

    BufferStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static BufferStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (BufferStatusEnum e : BufferStatusEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
