package com.bank.core.common.enums;

import lombok.Getter;

@Getter
public enum ResultCodeEnum {

    SUCCESS(200, "成功"),
    SYSTEM_ERROR(500, "系统异常"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),

    ACCOUNT_NOT_EXIST(1001, "账户不存在"),
    ACCOUNT_ALREADY_EXIST(1002, "账户已存在"),
    ACCOUNT_FROZEN(1003, "账户已冻结"),
    ACCOUNT_CLOSED(1004, "账户已销户"),
    INSUFFICIENT_BALANCE(1005, "余额不足"),
    INVALID_ACCOUNT_STATUS(1006, "账户状态无效"),
    CURRENCY_NOT_SUPPORTED(1007, "不支持的币种"),
    ACCOUNT_TYPE_NOT_SUPPORTED(1008, "不支持的账户类型"),
    FREEZE_TYPE_NOT_SUPPORTED(1009, "不支持的冻结类型"),
    ACCOUNT_NOT_FROZEN(1010, "账户未冻结"),

    DUPLICATE_REQUEST(2001, "重复请求"),
    SERVICE_UNAVAILABLE(2002, "服务不可用"),
    TRANSACTION_FAILED(2003, "交易失败"),
    DISTRIBUTED_LOCK_FAILED(2004, "获取分布式锁失败");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
