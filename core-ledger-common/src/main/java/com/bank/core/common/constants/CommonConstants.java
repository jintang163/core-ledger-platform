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

    // ==================== 热点账户与高并发相关常量 ====================

    /** 默认影子账户分片数量 */
    Integer DEFAULT_SHARD_COUNT = 10;

    /** 最小影子账户分片数量 */
    Integer MIN_SHARD_COUNT = 3;

    /** 最大影子账户分片数量 */
    Integer MAX_SHARD_COUNT = 100;

    /** 影子账户ID后缀前缀 */
    String SHARD_ID_SUFFIX = "_SHARD_";

    /** 热点账户锁前缀 */
    String HOT_ACCOUNT_LOCK_PREFIX = "account:hot:lock:";

    /** 影子账户缓存前缀 */
    String SHARD_ACCOUNT_CACHE_PREFIX = "account:shard:";

    /** 热点账户检测阈值（每秒交易数） */
    Integer HOT_ACCOUNT_THRESHOLD = 100;

    /** 账户级分布式锁前缀 */
    String ACCOUNT_BALANCE_LOCK_PREFIX = "account:balance:lock:";

    /** 账户级锁等待时间（秒） */
    Long ACCOUNT_LOCK_WAIT_TIME = 1L;

    /** 账户级锁持有时间（秒） */
    Long ACCOUNT_LOCK_LEASE_TIME = 5L;

    /** ==================== 重试与退避相关常量 ==================== */

    /** 乐观锁最大重试次数 */
    Integer OPTIMISTIC_LOCK_MAX_RETRY = 5;

    /** 指数退避初始延迟（毫秒） */
    Long RETRY_INITIAL_DELAY = 100L;

    /** 指数退避最大延迟（毫秒） */
    Long RETRY_MAX_DELAY = 5000L;

    /** 指数退避乘数 */
    Double RETRY_BACKOFF_MULTIPLIER = 2.0;

    /** ==================== 缓冲记账相关常量 ==================== */

    /** 缓冲记账流水缓存前缀 */
    String BUFFER_LOG_CACHE_PREFIX = "account:buffer:";

    /** 缓冲记账批量处理大小 */
    Integer BUFFER_BATCH_SIZE = 100;

    /** 缓冲记账定时任务间隔（毫秒） */
    Long BUFFER_PROCESS_INTERVAL = 1000L;

    /** 缓冲记账最大延迟处理时间（秒） */
    Integer BUFFER_MAX_DELAY = 5;

    /** 影子账户归并定时任务Cron */
    String SHARD_MERGE_CRON = "0 0 2 * * ?";

    /** 影子账户归并锁前缀 */
    String SHARD_MERGE_LOCK_PREFIX = "account:shard:merge:";
}
