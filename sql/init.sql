-- =====================================================
-- 核心账本平台数据库初始化脚本
-- 支持分库分表：core_ledger_0 和 core_ledger_1
-- =====================================================

CREATE DATABASE IF NOT EXISTS core_ledger_0 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS core_ledger_1 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================
-- 数据库：core_ledger_0
-- =====================================================
USE core_ledger_0;

-- -----------------------------------------------------
-- 账户表 t_account
-- 增加热点账户相关字段：hot_status, shard_count, sharding_strategy
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS t_account (
    id BIGINT NOT NULL COMMENT '主键ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账户号码',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    account_type TINYINT NOT NULL COMMENT '账户类型：1-个人，2-企业',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    balance BIGINT NOT NULL DEFAULT 0 COMMENT '余额（分）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-冻结，3-已销户',
    freeze_type TINYINT DEFAULT NULL COMMENT '冻结类型：1-司法冻结，2-风控冻结',
    freeze_remark VARCHAR(255) DEFAULT NULL COMMENT '冻结备注',
    freeze_time DATETIME DEFAULT NULL COMMENT '冻结时间',
    freeze_operator VARCHAR(64) DEFAULT NULL COMMENT '冻结操作人',
    open_time DATETIME DEFAULT NULL COMMENT '开户时间',
    close_time DATETIME DEFAULT NULL COMMENT '销户时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    hot_status TINYINT NOT NULL DEFAULT 0 COMMENT '热点账户状态：0-普通账户，1-待标记热点，2-已分片热点，3-已启用缓冲记账',
    shard_count INT DEFAULT NULL COMMENT '影子账户分片数量（热点账户专用）',
    sharding_strategy TINYINT DEFAULT NULL COMMENT '分片策略：1-随机路由，2-轮询路由，3-哈希路由',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_id (account_id),
    UNIQUE KEY uk_account_no (account_no),
    KEY idx_user_id (user_id),
    KEY idx_user_type_currency (user_id, account_type, currency),
    KEY idx_hot_status (hot_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户表';

-- 兼容已存在表的情况，添加缺失字段
ALTER TABLE t_account 
    ADD COLUMN IF NOT EXISTS hot_status TINYINT NOT NULL DEFAULT 0 COMMENT '热点账户状态：0-普通账户，1-待标记热点，2-已分片热点，3-已启用缓冲记账' AFTER version,
    ADD COLUMN IF NOT EXISTS shard_count INT DEFAULT NULL COMMENT '影子账户分片数量（热点账户专用）' AFTER hot_status,
    ADD COLUMN IF NOT EXISTS sharding_strategy TINYINT DEFAULT NULL COMMENT '分片策略：1-随机路由，2-轮询路由，3-哈希路由' AFTER shard_count,
    ADD INDEX IF NOT EXISTS idx_hot_status (hot_status);

-- -----------------------------------------------------
-- 账户影子分片表 t_account_shard
-- 用于热点账户的分片处理，将热点账户拆分为多个影子账户
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS t_account_shard (
    id BIGINT NOT NULL COMMENT '主键ID',
    shard_id VARCHAR(64) NOT NULL COMMENT '分片ID（账户ID + 分片索引）',
    main_account_id VARCHAR(64) NOT NULL COMMENT '主账户ID（关联的热点账户ID）',
    shard_index INT NOT NULL COMMENT '分片索引（从0开始）',
    shard_account_no VARCHAR(32) NOT NULL COMMENT '分片账户号',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    balance BIGINT NOT NULL DEFAULT 0 COMMENT '余额（分）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '分片状态：0-正常，1-已关闭',
    last_merge_time DATETIME DEFAULT NULL COMMENT '最后归并时间',
    merge_status TINYINT NOT NULL DEFAULT 0 COMMENT '归并状态：0-未归并，1-归并中，2-已归并',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_shard_id (shard_id),
    KEY idx_main_account_id (main_account_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户影子分片表（热点账户专用）';

-- -----------------------------------------------------
-- 账户缓冲记账流水表 t_account_buffer_log
-- 用于高并发场景下的缓冲记账：先记录流水，后异步批量更新余额
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS t_account_buffer_log (
    id BIGINT NOT NULL COMMENT '主键ID',
    buffer_id VARCHAR(64) NOT NULL COMMENT '缓冲流水ID',
    business_no VARCHAR(64) NOT NULL COMMENT '业务流水号（调用方传入）',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求ID（用于幂等）',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账户号',
    amount DECIMAL(18,2) NOT NULL COMMENT '变动金额（元，正数增加，负数减少）',
    amount_fen BIGINT NOT NULL COMMENT '变动金额（分）',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    transaction_type TINYINT NOT NULL COMMENT '交易类型：1-充值，2-提现，3-转账，4-批量转账',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-处理中，2-处理成功，3-处理失败',
    batch_no VARCHAR(64) DEFAULT NULL COMMENT '处理批次号',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    process_time DATETIME DEFAULT NULL COMMENT '处理完成时间',
    transaction_id VARCHAR(64) DEFAULT NULL COMMENT '关联的交易ID',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作员',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_buffer_id (buffer_id),
    UNIQUE KEY uk_request_id (request_id),
    UNIQUE KEY uk_business_no (business_no),
    KEY idx_account_id (account_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户缓冲记账流水表（高并发专用）';

-- -----------------------------------------------------
-- 账户冻结日志表（分表：t_account_freeze_log_0 ~ 3）
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS t_account_freeze_log_0 (
    id BIGINT NOT NULL COMMENT '主键ID',
    log_id VARCHAR(64) NOT NULL COMMENT '日志ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    operate_type TINYINT NOT NULL COMMENT '操作类型：1-冻结，2-解冻',
    freeze_type TINYINT NOT NULL COMMENT '冻结类型：1-司法冻结，2-风控冻结',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_id (log_id),
    KEY idx_account_id (account_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户冻结日志表_0';

CREATE TABLE IF NOT EXISTS t_account_freeze_log_1 (
    id BIGINT NOT NULL COMMENT '主键ID',
    log_id VARCHAR(64) NOT NULL COMMENT '日志ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    operate_type TINYINT NOT NULL COMMENT '操作类型：1-冻结，2-解冻',
    freeze_type TINYINT NOT NULL COMMENT '冻结类型：1-司法冻结，2-风控冻结',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_id (log_id),
    KEY idx_account_id (account_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户冻结日志表_1';

CREATE TABLE IF NOT EXISTS t_account_freeze_log_2 (
    id BIGINT NOT NULL COMMENT '主键ID',
    log_id VARCHAR(64) NOT NULL COMMENT '日志ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    operate_type TINYINT NOT NULL COMMENT '操作类型：1-冻结，2-解冻',
    freeze_type TINYINT NOT NULL COMMENT '冻结类型：1-司法冻结，2-风控冻结',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_id (log_id),
    KEY idx_account_id (account_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户冻结日志表_2';

CREATE TABLE IF NOT EXISTS t_account_freeze_log_3 (
    id BIGINT NOT NULL COMMENT '主键ID',
    log_id VARCHAR(64) NOT NULL COMMENT '日志ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    operate_type TINYINT NOT NULL COMMENT '操作类型：1-冻结，2-解冻',
    freeze_type TINYINT NOT NULL COMMENT '冻结类型：1-司法冻结，2-风控冻结',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_id (log_id),
    KEY idx_account_id (account_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户冻结日志表_3';

-- -----------------------------------------------------
-- 交易表（分表：t_transaction_0 ~ 3）
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS t_transaction_0 (
    id BIGINT NOT NULL COMMENT '主键ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    transaction_no VARCHAR(32) NOT NULL COMMENT '交易流水号',
    transaction_type TINYINT NOT NULL COMMENT '交易类型：1-转账，2-存款，3-取款，4-手续费，5-利息，6-调账',
    business_no VARCHAR(64) NOT NULL COMMENT '关联业务单号',
    total_amount DECIMAL(18,2) NOT NULL COMMENT '交易金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    voucher_no VARCHAR(32) NOT NULL COMMENT '记账凭证号',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-成功，2-失败，3-已冲正',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    transaction_time DATETIME NOT NULL COMMENT '交易时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_id (transaction_id),
    UNIQUE KEY uk_transaction_no (transaction_no),
    UNIQUE KEY uk_business_no (business_no),
    KEY idx_transaction_time (transaction_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易表_0';

CREATE TABLE IF NOT EXISTS t_transaction_1 (
    id BIGINT NOT NULL COMMENT '主键ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    transaction_no VARCHAR(32) NOT NULL COMMENT '交易流水号',
    transaction_type TINYINT NOT NULL COMMENT '交易类型：1-转账，2-存款，3-取款，4-手续费，5-利息，6-调账',
    business_no VARCHAR(64) NOT NULL COMMENT '关联业务单号',
    total_amount DECIMAL(18,2) NOT NULL COMMENT '交易金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    voucher_no VARCHAR(32) NOT NULL COMMENT '记账凭证号',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-成功，2-失败，3-已冲正',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    transaction_time DATETIME NOT NULL COMMENT '交易时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_id (transaction_id),
    UNIQUE KEY uk_transaction_no (transaction_no),
    UNIQUE KEY uk_business_no (business_no),
    KEY idx_transaction_time (transaction_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易表_1';

CREATE TABLE IF NOT EXISTS t_transaction_2 (
    id BIGINT NOT NULL COMMENT '主键ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    transaction_no VARCHAR(32) NOT NULL COMMENT '交易流水号',
    transaction_type TINYINT NOT NULL COMMENT '交易类型：1-转账，2-存款，3-取款，4-手续费，5-利息，6-调账',
    business_no VARCHAR(64) NOT NULL COMMENT '关联业务单号',
    total_amount DECIMAL(18,2) NOT NULL COMMENT '交易金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    voucher_no VARCHAR(32) NOT NULL COMMENT '记账凭证号',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-成功，2-失败，3-已冲正',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    transaction_time DATETIME NOT NULL COMMENT '交易时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_id (transaction_id),
    UNIQUE KEY uk_transaction_no (transaction_no),
    UNIQUE KEY uk_business_no (business_no),
    KEY idx_transaction_time (transaction_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易表_2';

CREATE TABLE IF NOT EXISTS t_transaction_3 (
    id BIGINT NOT NULL COMMENT '主键ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    transaction_no VARCHAR(32) NOT NULL COMMENT '交易流水号',
    transaction_type TINYINT NOT NULL COMMENT '交易类型：1-转账，2-存款，3-取款，4-手续费，5-利息，6-调账',
    business_no VARCHAR(64) NOT NULL COMMENT '关联业务单号',
    total_amount DECIMAL(18,2) NOT NULL COMMENT '交易金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    voucher_no VARCHAR(32) NOT NULL COMMENT '记账凭证号',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-成功，2-失败，3-已冲正',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    transaction_time DATETIME NOT NULL COMMENT '交易时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_id (transaction_id),
    UNIQUE KEY uk_transaction_no (transaction_no),
    UNIQUE KEY uk_business_no (business_no),
    KEY idx_transaction_time (transaction_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易表_3';

-- -----------------------------------------------------
-- 交易分录表（分表：t_transaction_entry_0 ~ 3）
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS t_transaction_entry_0 (
    id BIGINT NOT NULL COMMENT '主键ID',
    entry_id VARCHAR(64) NOT NULL COMMENT '分录ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账户号码',
    subject_code VARCHAR(32) NOT NULL COMMENT '科目代码',
    subject_name VARCHAR(64) NOT NULL COMMENT '科目名称',
    direction TINYINT NOT NULL COMMENT '借贷方向：1-借，2-贷',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_entry_id (entry_id),
    KEY idx_transaction_id (transaction_id),
    KEY idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易分录表_0';

CREATE TABLE IF NOT EXISTS t_transaction_entry_1 (
    id BIGINT NOT NULL COMMENT '主键ID',
    entry_id VARCHAR(64) NOT NULL COMMENT '分录ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账户号码',
    subject_code VARCHAR(32) NOT NULL COMMENT '科目代码',
    subject_name VARCHAR(64) NOT NULL COMMENT '科目名称',
    direction TINYINT NOT NULL COMMENT '借贷方向：1-借，2-贷',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_entry_id (entry_id),
    KEY idx_transaction_id (transaction_id),
    KEY idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易分录表_1';

CREATE TABLE IF NOT EXISTS t_transaction_entry_2 (
    id BIGINT NOT NULL COMMENT '主键ID',
    entry_id VARCHAR(64) NOT NULL COMMENT '分录ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账户号码',
    subject_code VARCHAR(32) NOT NULL COMMENT '科目代码',
    subject_name VARCHAR(64) NOT NULL COMMENT '科目名称',
    direction TINYINT NOT NULL COMMENT '借贷方向：1-借，2-贷',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_entry_id (entry_id),
    KEY idx_transaction_id (transaction_id),
    KEY idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易分录表_2';

CREATE TABLE IF NOT EXISTS t_transaction_entry_3 (
    id BIGINT NOT NULL COMMENT '主键ID',
    entry_id VARCHAR(64) NOT NULL COMMENT '分录ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账户号码',
    subject_code VARCHAR(32) NOT NULL COMMENT '科目代码',
    subject_name VARCHAR(64) NOT NULL COMMENT '科目名称',
    direction TINYINT NOT NULL COMMENT '借贷方向：1-借，2-贷',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_entry_id (entry_id),
    KEY idx_transaction_id (transaction_id),
    KEY idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易分录表_3';

-- -----------------------------------------------------
-- Seata AT模式回滚日志表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id BIGINT NOT NULL COMMENT '分支事务ID',
    xid VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    context VARCHAR(128) NOT NULL COMMENT '上下文',
    rollback_info LONGBLOB NOT NULL COMMENT '回滚信息',
    log_status INT NOT NULL COMMENT '日志状态：0-正常，1-已回滚',
    log_created DATETIME NOT NULL COMMENT '创建时间',
    log_modified DATETIME NOT NULL COMMENT '修改时间',
    PRIMARY KEY (branch_id),
    UNIQUE KEY ux_xid_branch_id (xid, branch_id),
    KEY ix_log_status (log_status),
    KEY ix_log_created (log_created)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seata AT模式回滚日志表';

-- =====================================================
-- 数据库：core_ledger_1
-- =====================================================
USE core_ledger_1;

-- -----------------------------------------------------
-- 账户表 t_account
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS t_account (
    id BIGINT NOT NULL COMMENT '主键ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账户号码',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    account_type TINYINT NOT NULL COMMENT '账户类型：1-个人，2-企业',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    balance BIGINT NOT NULL DEFAULT 0 COMMENT '余额（分）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-冻结，3-已销户',
    freeze_type TINYINT DEFAULT NULL COMMENT '冻结类型：1-司法冻结，2-风控冻结',
    freeze_remark VARCHAR(255) DEFAULT NULL COMMENT '冻结备注',
    freeze_time DATETIME DEFAULT NULL COMMENT '冻结时间',
    freeze_operator VARCHAR(64) DEFAULT NULL COMMENT '冻结操作人',
    open_time DATETIME DEFAULT NULL COMMENT '开户时间',
    close_time DATETIME DEFAULT NULL COMMENT '销户时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    hot_status TINYINT NOT NULL DEFAULT 0 COMMENT '热点账户状态：0-普通账户，1-待标记热点，2-已分片热点，3-已启用缓冲记账',
    shard_count INT DEFAULT NULL COMMENT '影子账户分片数量（热点账户专用）',
    sharding_strategy TINYINT DEFAULT NULL COMMENT '分片策略：1-随机路由，2-轮询路由，3-哈希路由',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_id (account_id),
    UNIQUE KEY uk_account_no (account_no),
    KEY idx_user_id (user_id),
    KEY idx_user_type_currency (user_id, account_type, currency),
    KEY idx_hot_status (hot_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户表';

-- 兼容已存在表的情况，添加缺失字段
ALTER TABLE t_account 
    ADD COLUMN IF NOT EXISTS hot_status TINYINT NOT NULL DEFAULT 0 COMMENT '热点账户状态：0-普通账户，1-待标记热点，2-已分片热点，3-已启用缓冲记账' AFTER version,
    ADD COLUMN IF NOT EXISTS shard_count INT DEFAULT NULL COMMENT '影子账户分片数量（热点账户专用）' AFTER hot_status,
    ADD COLUMN IF NOT EXISTS sharding_strategy TINYINT DEFAULT NULL COMMENT '分片策略：1-随机路由，2-轮询路由，3-哈希路由' AFTER shard_count,
    ADD INDEX IF NOT EXISTS idx_hot_status (hot_status);

-- -----------------------------------------------------
-- 账户影子分片表 t_account_shard
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS t_account_shard (
    id BIGINT NOT NULL COMMENT '主键ID',
    shard_id VARCHAR(64) NOT NULL COMMENT '分片ID（账户ID + 分片索引）',
    main_account_id VARCHAR(64) NOT NULL COMMENT '主账户ID（关联的热点账户ID）',
    shard_index INT NOT NULL COMMENT '分片索引（从0开始）',
    shard_account_no VARCHAR(32) NOT NULL COMMENT '分片账户号',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    balance BIGINT NOT NULL DEFAULT 0 COMMENT '余额（分）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '分片状态：0-正常，1-已关闭',
    last_merge_time DATETIME DEFAULT NULL COMMENT '最后归并时间',
    merge_status TINYINT NOT NULL DEFAULT 0 COMMENT '归并状态：0-未归并，1-归并中，2-已归并',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_shard_id (shard_id),
    KEY idx_main_account_id (main_account_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户影子分片表（热点账户专用）';

-- -----------------------------------------------------
-- 账户缓冲记账流水表 t_account_buffer_log
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS t_account_buffer_log (
    id BIGINT NOT NULL COMMENT '主键ID',
    buffer_id VARCHAR(64) NOT NULL COMMENT '缓冲流水ID',
    business_no VARCHAR(64) NOT NULL COMMENT '业务流水号（调用方传入）',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求ID（用于幂等）',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账户号',
    amount DECIMAL(18,2) NOT NULL COMMENT '变动金额（元，正数增加，负数减少）',
    amount_fen BIGINT NOT NULL COMMENT '变动金额（分）',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    transaction_type TINYINT NOT NULL COMMENT '交易类型：1-充值，2-提现，3-转账，4-批量转账',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-处理中，2-处理成功，3-处理失败',
    batch_no VARCHAR(64) DEFAULT NULL COMMENT '处理批次号',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    process_time DATETIME DEFAULT NULL COMMENT '处理完成时间',
    transaction_id VARCHAR(64) DEFAULT NULL COMMENT '关联的交易ID',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作员',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_buffer_id (buffer_id),
    UNIQUE KEY uk_request_id (request_id),
    UNIQUE KEY uk_business_no (business_no),
    KEY idx_account_id (account_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户缓冲记账流水表（高并发专用）';

-- -----------------------------------------------------
-- 账户冻结日志表（分表：t_account_freeze_log_0 ~ 3）
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS t_account_freeze_log_0 (
    id BIGINT NOT NULL COMMENT '主键ID',
    log_id VARCHAR(64) NOT NULL COMMENT '日志ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    operate_type TINYINT NOT NULL COMMENT '操作类型：1-冻结，2-解冻',
    freeze_type TINYINT NOT NULL COMMENT '冻结类型：1-司法冻结，2-风控冻结',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_id (log_id),
    KEY idx_account_id (account_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户冻结日志表_0';

CREATE TABLE IF NOT EXISTS t_account_freeze_log_1 (
    id BIGINT NOT NULL COMMENT '主键ID',
    log_id VARCHAR(64) NOT NULL COMMENT '日志ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    operate_type TINYINT NOT NULL COMMENT '操作类型：1-冻结，2-解冻',
    freeze_type TINYINT NOT NULL COMMENT '冻结类型：1-司法冻结，2-风控冻结',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_id (log_id),
    KEY idx_account_id (account_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户冻结日志表_1';

CREATE TABLE IF NOT EXISTS t_account_freeze_log_2 (
    id BIGINT NOT NULL COMMENT '主键ID',
    log_id VARCHAR(64) NOT NULL COMMENT '日志ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    operate_type TINYINT NOT NULL COMMENT '操作类型：1-冻结，2-解冻',
    freeze_type TINYINT NOT NULL COMMENT '冻结类型：1-司法冻结，2-风控冻结',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_id (log_id),
    KEY idx_account_id (account_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户冻结日志表_2';

CREATE TABLE IF NOT EXISTS t_account_freeze_log_3 (
    id BIGINT NOT NULL COMMENT '主键ID',
    log_id VARCHAR(64) NOT NULL COMMENT '日志ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    operate_type TINYINT NOT NULL COMMENT '操作类型：1-冻结，2-解冻',
    freeze_type TINYINT NOT NULL COMMENT '冻结类型：1-司法冻结，2-风控冻结',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_id (log_id),
    KEY idx_account_id (account_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户冻结日志表_3';

-- -----------------------------------------------------
-- 交易表（分表：t_transaction_0 ~ 3）
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS t_transaction_0 (
    id BIGINT NOT NULL COMMENT '主键ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    transaction_no VARCHAR(32) NOT NULL COMMENT '交易流水号',
    transaction_type TINYINT NOT NULL COMMENT '交易类型：1-转账，2-存款，3-取款，4-手续费，5-利息，6-调账',
    business_no VARCHAR(64) NOT NULL COMMENT '关联业务单号',
    total_amount DECIMAL(18,2) NOT NULL COMMENT '交易金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    voucher_no VARCHAR(32) NOT NULL COMMENT '记账凭证号',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-成功，2-失败，3-已冲正',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    transaction_time DATETIME NOT NULL COMMENT '交易时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_id (transaction_id),
    UNIQUE KEY uk_transaction_no (transaction_no),
    UNIQUE KEY uk_business_no (business_no),
    KEY idx_transaction_time (transaction_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易表_0';

CREATE TABLE IF NOT EXISTS t_transaction_1 (
    id BIGINT NOT NULL COMMENT '主键ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    transaction_no VARCHAR(32) NOT NULL COMMENT '交易流水号',
    transaction_type TINYINT NOT NULL COMMENT '交易类型：1-转账，2-存款，3-取款，4-手续费，5-利息，6-调账',
    business_no VARCHAR(64) NOT NULL COMMENT '关联业务单号',
    total_amount DECIMAL(18,2) NOT NULL COMMENT '交易金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    voucher_no VARCHAR(32) NOT NULL COMMENT '记账凭证号',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-成功，2-失败，3-已冲正',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    transaction_time DATETIME NOT NULL COMMENT '交易时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_id (transaction_id),
    UNIQUE KEY uk_transaction_no (transaction_no),
    UNIQUE KEY uk_business_no (business_no),
    KEY idx_transaction_time (transaction_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易表_1';

CREATE TABLE IF NOT EXISTS t_transaction_2 (
    id BIGINT NOT NULL COMMENT '主键ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    transaction_no VARCHAR(32) NOT NULL COMMENT '交易流水号',
    transaction_type TINYINT NOT NULL COMMENT '交易类型：1-转账，2-存款，3-取款，4-手续费，5-利息，6-调账',
    business_no VARCHAR(64) NOT NULL COMMENT '关联业务单号',
    total_amount DECIMAL(18,2) NOT NULL COMMENT '交易金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    voucher_no VARCHAR(32) NOT NULL COMMENT '记账凭证号',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-成功，2-失败，3-已冲正',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    transaction_time DATETIME NOT NULL COMMENT '交易时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_id (transaction_id),
    UNIQUE KEY uk_transaction_no (transaction_no),
    UNIQUE KEY uk_business_no (business_no),
    KEY idx_transaction_time (transaction_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易表_2';

CREATE TABLE IF NOT EXISTS t_transaction_3 (
    id BIGINT NOT NULL COMMENT '主键ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    transaction_no VARCHAR(32) NOT NULL COMMENT '交易流水号',
    transaction_type TINYINT NOT NULL COMMENT '交易类型：1-转账，2-存款，3-取款，4-手续费，5-利息，6-调账',
    business_no VARCHAR(64) NOT NULL COMMENT '关联业务单号',
    total_amount DECIMAL(18,2) NOT NULL COMMENT '交易金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    voucher_no VARCHAR(32) NOT NULL COMMENT '记账凭证号',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-成功，2-失败，3-已冲正',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    transaction_time DATETIME NOT NULL COMMENT '交易时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_id (transaction_id),
    UNIQUE KEY uk_transaction_no (transaction_no),
    UNIQUE KEY uk_business_no (business_no),
    KEY idx_transaction_time (transaction_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易表_3';

-- -----------------------------------------------------
-- 交易分录表（分表：t_transaction_entry_0 ~ 3）
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS t_transaction_entry_0 (
    id BIGINT NOT NULL COMMENT '主键ID',
    entry_id VARCHAR(64) NOT NULL COMMENT '分录ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账户号码',
    subject_code VARCHAR(32) NOT NULL COMMENT '科目代码',
    subject_name VARCHAR(64) NOT NULL COMMENT '科目名称',
    direction TINYINT NOT NULL COMMENT '借贷方向：1-借，2-贷',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_entry_id (entry_id),
    KEY idx_transaction_id (transaction_id),
    KEY idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易分录表_0';

CREATE TABLE IF NOT EXISTS t_transaction_entry_1 (
    id BIGINT NOT NULL COMMENT '主键ID',
    entry_id VARCHAR(64) NOT NULL COMMENT '分录ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账户号码',
    subject_code VARCHAR(32) NOT NULL COMMENT '科目代码',
    subject_name VARCHAR(64) NOT NULL COMMENT '科目名称',
    direction TINYINT NOT NULL COMMENT '借贷方向：1-借，2-贷',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_entry_id (entry_id),
    KEY idx_transaction_id (transaction_id),
    KEY idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易分录表_1';

CREATE TABLE IF NOT EXISTS t_transaction_entry_2 (
    id BIGINT NOT NULL COMMENT '主键ID',
    entry_id VARCHAR(64) NOT NULL COMMENT '分录ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账户号码',
    subject_code VARCHAR(32) NOT NULL COMMENT '科目代码',
    subject_name VARCHAR(64) NOT NULL COMMENT '科目名称',
    direction TINYINT NOT NULL COMMENT '借贷方向：1-借，2-贷',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_entry_id (entry_id),
    KEY idx_transaction_id (transaction_id),
    KEY idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易分录表_2';

CREATE TABLE IF NOT EXISTS t_transaction_entry_3 (
    id BIGINT NOT NULL COMMENT '主键ID',
    entry_id VARCHAR(64) NOT NULL COMMENT '分录ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '交易ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账户号码',
    subject_code VARCHAR(32) NOT NULL COMMENT '科目代码',
    subject_name VARCHAR(64) NOT NULL COMMENT '科目名称',
    direction TINYINT NOT NULL COMMENT '借贷方向：1-借，2-贷',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    summary VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_entry_id (entry_id),
    KEY idx_transaction_id (transaction_id),
    KEY idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易分录表_3';

-- -----------------------------------------------------
-- Seata AT模式回滚日志表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id BIGINT NOT NULL COMMENT '分支事务ID',
    xid VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    context VARCHAR(128) NOT NULL COMMENT '上下文',
    rollback_info LONGBLOB NOT NULL COMMENT '回滚信息',
    log_status INT NOT NULL COMMENT '日志状态：0-正常，1-已回滚',
    log_created DATETIME NOT NULL COMMENT '创建时间',
    log_modified DATETIME NOT NULL COMMENT '修改时间',
    PRIMARY KEY (branch_id),
    UNIQUE KEY ux_xid_branch_id (xid, branch_id),
    KEY ix_log_status (log_status),
    KEY ix_log_created (log_created)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seata AT模式回滚日志表';
