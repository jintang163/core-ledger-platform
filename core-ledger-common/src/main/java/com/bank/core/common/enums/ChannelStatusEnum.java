package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum ChannelStatusEnum {

    PENDING(0, "待渠道处理"),
    CHANNEL_SUCCESS(1, "渠道处理成功"),
    CHANNEL_FAILED(2, "渠道处理失败"),
    CALLBACK_SUCCESS(3, "回调成功"),
    CALLBACK_FAILED(4, "回调失败");

    private final Integer code;
    private final String desc;

    ChannelStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ChannelStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ChannelStatusEnum e : ChannelStatusEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
