package com.bank.core.common.constants;

public interface CommonConstants {

    String DEFAULT_CHARSET = "UTF-8";

    String DATE_FORMAT = "yyyy-MM-dd";
    String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    Long DEFAULT_LOCK_WAIT_TIME = 3L;
    Long DEFAULT_LOCK_LEASE_TIME = 30L;

    String ACCOUNT_CACHE_PREFIX = "account:";
    String ACCOUNT_LOCK_PREFIX = "account:lock:";
    String ACCOUNT_IDEMPOTENT_PREFIX = "account:idempotent:";

    String ROCKETMQ_TOPIC_ACCOUNT = "account-topic";
    String ROCKETMQ_TAG_ACCOUNT_CREATE = "create";
    String ROCKETMQ_TAG_ACCOUNT_FREEZE = "freeze";
    String ROCKETMQ_TAG_ACCOUNT_UNFREEZE = "unfreeze";
    String ROCKETMQ_TAG_ACCOUNT_CLOSE = "close";
    String ROCKETMQ_TAG_TRANSACTION = "transaction";

    String TRANSACTION_IDEMPOTENT_PREFIX = "transaction:idempotent:";
    String TRANSACTION_LOCK_PREFIX = "transaction:lock:";
    String TRANSACTION_CACHE_PREFIX = "transaction:";

    String PAYMENT_IDEMPOTENT_PREFIX = "payment:idempotent:";
    String PAYMENT_LOCK_PREFIX = "payment:lock:";
    String PAYMENT_CACHE_PREFIX = "payment:";

    String TRANSFER_IDEMPOTENT_PREFIX = "transfer:idempotent:";
    String TRANSFER_LOCK_PREFIX = "transfer:lock:";
    String TRANSFER_CACHE_PREFIX = "transfer:";

    String BATCH_TRANSFER_IDEMPOTENT_PREFIX = "batch_transfer:idempotent:";
    String BATCH_TRANSFER_LOCK_PREFIX = "batch_transfer:lock:";
    String BATCH_TRANSFER_CACHE_PREFIX = "batch_transfer:";

    String ROCKETMQ_TAG_PAYMENT_RECHARGE = "payment:recharge";
    String ROCKETMQ_TAG_PAYMENT_WITHDRAW = "payment:withdraw";
    String ROCKETMQ_TAG_TRANSFER = "transfer";
    String ROCKETMQ_TAG_BATCH_TRANSFER = "batch_transfer";
    String ROCKETMQ_TAG_CHANNEL_CALLBACK = "channel:callback";

    Integer MAX_RETRY_TIMES = 3;

    Integer MAX_BATCH_TRANSFER_SIZE = 1000;

    String SEATA_TX_GROUP = "account-tx-group";

    Integer DEFAULT_PAGE_SIZE = 10;
    Integer MAX_PAGE_SIZE = 100;

    Long MIN_BALANCE = 0L;
    Long MAX_BALANCE = 999999999999999L;

    String TRACE_ID = "traceId";
    String REQUEST_ID = "requestId";
    String USER_ID = "userId";
}
