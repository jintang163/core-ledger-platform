CREATE DATABASE IF NOT EXISTS core_ledger_0 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS core_ledger_1 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE core_ledger_0;

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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_id (account_id),
    UNIQUE KEY uk_account_no (account_no),
    KEY idx_user_id (user_id),
    KEY idx_user_type_currency (user_id, account_type, currency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户表';

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

USE core_ledger_1;

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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_id (account_id),
    UNIQUE KEY uk_account_no (account_no),
    KEY idx_user_id (user_id),
    KEY idx_user_type_currency (user_id, account_type, currency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户表';

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
