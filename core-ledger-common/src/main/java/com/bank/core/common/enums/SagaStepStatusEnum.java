package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum SagaStepStatusEnum {

    PENDING(0, "待执行"),
    FORWARD_SUCCESS(1, "正向执行成功"),
    FORWARD_FAILED(2, "正向执行失败"),
    COMPENSATE_SUCCESS(3, "补偿执行成功"),
    COMPENSATE_FAILED(4, "补偿执行失败"),
    SKIPPED(5, "已跳过");

    private final Integer code;
    private final String desc;

    SagaStepStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SagaStepStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SagaStepStatusEnum e : SagaStepStatusEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
