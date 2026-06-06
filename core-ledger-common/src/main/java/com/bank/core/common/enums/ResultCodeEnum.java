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
    DISTRIBUTED_LOCK_FAILED(2004, "获取分布式锁失败"),

    TRANSACTION_NOT_EXIST(3001, "交易不存在"),
    DEBIT_CREDIT_NOT_BALANCE(3002, "借贷不平衡"),
    INVALID_DIRECTION(3003, "无效的借贷方向"),
    INVALID_TRANSACTION_TYPE(3004, "无效的交易类型"),
    CONCURRENT_UPDATE_FAILED(3005, "并发更新失败，请重试"),
    INSUFFICIENT_ENTRY(3006, "至少需要两条分录"),

    PAYMENT_ORDER_NOT_EXIST(4001, "支付订单不存在"),
    INVALID_PAYMENT_TYPE(4002, "无效的支付类型"),
    INVALID_PAYMENT_STATUS(4003, "无效的支付状态"),
    PAYMENT_ORDER_ALREADY_PROCESSED(4004, "支付订单已处理"),
    CHANNEL_NOT_SUPPORTED(4005, "不支持的渠道"),
    INVALID_CHANNEL_STATUS(4006, "无效的渠道状态"),

    TRANSFER_ORDER_NOT_EXIST(5001, "转账订单不存在"),
    TRANSFER_SAME_ACCOUNT(5002, "转账方和收款方不能相同"),
    TRANSFER_CURRENCY_MISMATCH(5003, "转账币种不匹配"),

    BATCH_TRANSFER_EMPTY(6001, "批量转账明细不能为空"),
    BATCH_TRANSFER_TOO_MANY_ITEMS(6002, "批量转账明细超过最大条数"),
    BATCH_TRANSFER_ORDER_NOT_EXIST(6003, "批量转账订单不存在"),
    BATCH_TRANSFER_ITEM_NOT_EXIST(6004, "批量转账明细不存在"),

    BUFFER_LOG_NOT_EXIST(7001, "缓冲流水不存在"),
    BUFFER_LOG_RETRY_EXCEEDED(7002, "缓冲流水重试次数超限"),

    ACCOUNT_STATUS_ERROR(8001, "账户状态异常"),
    HOT_ACCOUNT_SHARD_FAILED(8002, "热点账户分片失败"),
    HOT_ACCOUNT_MERGE_FAILED(8003, "热点账户归并失败");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
