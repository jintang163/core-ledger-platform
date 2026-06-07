-- =====================================================
-- 核心账本平台 - 交易表按月分表创建脚本
-- 用于提前创建未来月份的交易表和交易分录表
-- 执行频率：建议每月最后一天执行一次，创建下一个月的表
-- =====================================================

DELIMITER //

-- =====================================================
-- 存储过程：创建指定月份的交易表和索引
-- =====================================================
CREATE PROCEDURE IF NOT EXISTS create_monthly_transaction_tables(
    IN year_month VARCHAR(6),  -- 格式：YYYYMM，如 202506
    IN db_name VARCHAR(64)     -- 数据库名
)
BEGIN
    DECLARE tx_table_name VARCHAR(64);
    DECLARE entry_table_name VARCHAR(64);
    DECLARE sql_stmt TEXT;
    
    SET tx_table_name = CONCAT('t_transaction_', year_month);
    SET entry_table_name = CONCAT('t_transaction_entry_', year_month);
    
    -- 切换数据库
    SET @use_sql = CONCAT('USE ', db_name);
    PREPARE stmt FROM @use_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    -- 创建交易表
    SET sql_stmt = CONCAT(
        'CREATE TABLE IF NOT EXISTS ', tx_table_name, ' (
            id BIGINT NOT NULL COMMENT ''主键ID'',
            transaction_id VARCHAR(64) NOT NULL COMMENT ''交易ID'',
            transaction_no VARCHAR(32) NOT NULL COMMENT ''交易流水号'',
            transaction_type TINYINT NOT NULL COMMENT ''交易类型：1-转账，2-存款，3-取款，4-手续费，5-利息，6-调账'',
            business_no VARCHAR(64) NOT NULL COMMENT ''关联业务单号'',
            total_amount DECIMAL(18,2) NOT NULL COMMENT ''交易金额'',
            currency VARCHAR(8) NOT NULL COMMENT ''币种'',
            voucher_no VARCHAR(32) NOT NULL COMMENT ''记账凭证号'',
            summary VARCHAR(255) DEFAULT NULL COMMENT ''摘要'',
            status TINYINT NOT NULL DEFAULT 0 COMMENT ''状态：0-待处理，1-成功，2-失败，3-已冲正'',
            request_id VARCHAR(64) DEFAULT NULL COMMENT ''请求ID'',
            operator VARCHAR(64) DEFAULT NULL COMMENT ''操作人'',
            transaction_time DATETIME NOT NULL COMMENT ''交易时间'',
            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
            update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
            deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除：0-未删除，1-已删除'',
            PRIMARY KEY (id),
            UNIQUE KEY uk_transaction_id (transaction_id),
            UNIQUE KEY uk_transaction_no (transaction_no),
            UNIQUE KEY uk_business_no (business_no),
            KEY idx_transaction_time (transaction_time),
            KEY idx_status (status),
            KEY idx_transaction_time_status (transaction_time, status),
            KEY idx_status_transaction_time (status, transaction_time),
            KEY idx_type_transaction_time (transaction_type, transaction_time)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''交易表_', year_month, ''''
    );
    
    PREPARE stmt FROM sql_stmt;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    -- 创建交易分录表
    SET sql_stmt = CONCAT(
        'CREATE TABLE IF NOT EXISTS ', entry_table_name, ' (
            id BIGINT NOT NULL COMMENT ''主键ID'',
            entry_id VARCHAR(64) NOT NULL COMMENT ''分录ID'',
            transaction_id VARCHAR(64) NOT NULL COMMENT ''交易ID'',
            account_id VARCHAR(64) NOT NULL COMMENT ''账户ID'',
            account_no VARCHAR(32) NOT NULL COMMENT ''账户号码'',
            subject_code VARCHAR(32) NOT NULL COMMENT ''科目代码'',
            subject_name VARCHAR(64) NOT NULL COMMENT ''科目名称'',
            direction TINYINT NOT NULL COMMENT ''借贷方向：1-借，2-贷'',
            amount DECIMAL(18,2) NOT NULL COMMENT ''金额'',
            currency VARCHAR(8) NOT NULL COMMENT ''币种'',
            summary VARCHAR(255) DEFAULT NULL COMMENT ''摘要'',
            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
            PRIMARY KEY (id),
            UNIQUE KEY uk_entry_id (entry_id),
            KEY idx_transaction_id (transaction_id),
            KEY idx_account_id (account_id),
            KEY idx_account_create_time (account_id, create_time),
            KEY idx_account_transaction (account_id, transaction_id),
            KEY idx_create_time (create_time)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''交易分录表_', year_month, ''''
    );
    
    PREPARE stmt FROM sql_stmt;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SELECT CONCAT('Created tables for ', year_month, ' in ', db_name) AS result;
END //

-- =====================================================
-- 存储过程：批量创建未来N个月的交易表
-- =====================================================
CREATE PROCEDURE IF NOT EXISTS create_future_monthly_tables(
    IN months_ahead INT,      -- 提前创建的月数
    IN db_name VARCHAR(64)    -- 数据库名
)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE current_date DATE;
    DECLARE target_date DATE;
    DECLARE year_month VARCHAR(6);
    
    SET current_date = CURDATE();
    
    WHILE i < months_ahead DO
        SET target_date = DATE_ADD(current_date, INTERVAL i MONTH);
        SET year_month = DATE_FORMAT(target_date, '%Y%m');
        CALL create_monthly_transaction_tables(year_month, db_name);
        SET i = i + 1;
    END WHILE;
    
    SELECT CONCAT('Created ', months_ahead, ' months of tables in ', db_name) AS result;
END //

-- =====================================================
-- 存储过程：创建指定月份在所有分库的表
-- =====================================================
CREATE PROCEDURE IF NOT EXISTS create_monthly_tables_all_shards(
    IN year_month VARCHAR(6)  -- 格式：YYYYMM
)
BEGIN
    CALL create_monthly_transaction_tables(year_month, 'core_ledger_0');
    CALL create_monthly_transaction_tables(year_month, 'core_ledger_1');
    SELECT CONCAT('Created tables for ', year_month, ' in all shards') AS result;
END //

-- =====================================================
-- 存储过程：创建未来N个月在所有分库的表
-- =====================================================
CREATE PROCEDURE IF NOT EXISTS create_future_tables_all_shards(
    IN months_ahead INT  -- 提前创建的月数
)
BEGIN
    CALL create_future_monthly_tables(months_ahead, 'core_ledger_0');
    CALL create_future_monthly_tables(months_ahead, 'core_ledger_1');
    SELECT CONCAT('Created ', months_ahead, ' months of tables in all shards') AS result;
END //

DELIMITER ;

-- =====================================================
-- 示例：创建未来3个月的表（在所有分库）
-- =====================================================
-- CALL create_future_tables_all_shards(3);

-- =====================================================
-- 示例：创建2025年7月的表（在所有分库）
-- =====================================================
-- CALL create_monthly_tables_all_shards('202507');
