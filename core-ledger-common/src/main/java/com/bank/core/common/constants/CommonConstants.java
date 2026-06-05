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

    String SEATA_TX_GROUP = "account-tx-group";

    Integer DEFAULT_PAGE_SIZE = 10;
    Integer MAX_PAGE_SIZE = 100;

    Long MIN_BALANCE = 0L;
    Long MAX_BALANCE = 999999999999999L;

    String TRACE_ID = "traceId";
    String REQUEST_ID = "requestId";
    String USER_ID = "userId";
}
