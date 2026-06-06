package com.bank.core.common.enums;

import lombok.Getter;

/**
 * 分片策略枚举
 * 用于热点账户分片时选择不同的路由策略
 */
@Getter
public enum ShardingStrategyEnum {

    RANDOM(1, "随机路由"),

    ROUND_ROBIN(2, "轮询路由"),

    HASH(3, "哈希路由");

    private final Integer code;
    private final String desc;

    ShardingStrategyEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ShardingStrategyEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ShardingStrategyEnum e : ShardingStrategyEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
