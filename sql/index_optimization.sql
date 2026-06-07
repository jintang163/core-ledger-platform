-- =====================================================
-- 核心账本平台数据库索引优化脚本
-- 针对分库分表后的查询性能优化
-- =====================================================

-- =====================================================
-- 数据库：core_ledger_0
-- =====================================================
USE core_ledger_0;

-- =====================================================
-- 交易表索引优化
-- 支持按交易时间、状态、类型的快速查询
-- =====================================================

-- 为交易表按月分表添加索引（需要为每个月份表执行）
-- 先创建一个存储过程来为所有交易分表添加索引

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS add_transaction_indexes(IN table_suffix VARCHAR(10))
BEGIN
    SET @table_name = CONCAT('t_transaction_', table_suffix);
    
    SET @sql = CONCAT('ALTER TABLE ', @table_name, 
        ' ADD INDEX IF NOT EXISTS idx_transaction_time_status (transaction_time, status),',
        ' ADD INDEX IF NOT EXISTS idx_status_transaction_time (status, transaction_time),',
        ' ADD INDEX IF NOT EXISTS idx_type_transaction_time (transaction_type, transaction_time)');
    
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SELECT CONCAT('Indexes added for ', @table_name) AS result;
END //

CREATE PROCEDURE IF NOT EXISTS add_transaction_entry_indexes(IN table_suffix VARCHAR(10))
BEGIN
    SET @table_name = CONCAT('t_transaction_entry_', table_suffix);
    
    SET @sql = CONCAT('ALTER TABLE ', @table_name,
        ' ADD INDEX IF NOT EXISTS idx_account_create_time (account_id, create_time),',
        ' ADD INDEX IF NOT EXISTS idx_account_transaction (account_id, transaction_id),',
        ' ADD INDEX IF NOT EXISTS idx_create_time (create_time)');
    
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SELECT CONCAT('Indexes added for ', @table_name) AS result;
END //

DELIMITER ;

-- 为2025年和2026年的月份表添加索引
CALL add_transaction_indexes('202501');
CALL add_transaction_indexes('202502');
CALL add_transaction_indexes('202503');
CALL add_transaction_indexes('202504');
CALL add_transaction_indexes('202505');
CALL add_transaction_indexes('202506');
CALL add_transaction_indexes('202507');
CALL add_transaction_indexes('202508');
CALL add_transaction_indexes('202509');
CALL add_transaction_indexes('202510');
CALL add_transaction_indexes('202511');
CALL add_transaction_indexes('202512');
CALL add_transaction_indexes('202601');
CALL add_transaction_indexes('202602');
CALL add_transaction_indexes('202603');
CALL add_transaction_indexes('202604');
CALL add_transaction_indexes('202605');
CALL add_transaction_indexes('202606');
CALL add_transaction_indexes('202607');
CALL add_transaction_indexes('202608');
CALL add_transaction_indexes('202609');
CALL add_transaction_indexes('202610');
CALL add_transaction_indexes('202611');
CALL add_transaction_indexes('202612');

CALL add_transaction_entry_indexes('202501');
CALL add_transaction_entry_indexes('202502');
CALL add_transaction_entry_indexes('202503');
CALL add_transaction_entry_indexes('202504');
CALL add_transaction_entry_indexes('202505');
CALL add_transaction_entry_indexes('202506');
CALL add_transaction_entry_indexes('202507');
CALL add_transaction_entry_indexes('202508');
CALL add_transaction_entry_indexes('202509');
CALL add_transaction_entry_indexes('202510');
CALL add_transaction_entry_indexes('202511');
CALL add_transaction_entry_indexes('202512');
CALL add_transaction_entry_indexes('202601');
CALL add_transaction_entry_indexes('202602');
CALL add_transaction_entry_indexes('202603');
CALL add_transaction_entry_indexes('202604');
CALL add_transaction_entry_indexes('202605');
CALL add_transaction_entry_indexes('202606');
CALL add_transaction_entry_indexes('202607');
CALL add_transaction_entry_indexes('202608');
CALL add_transaction_entry_indexes('202609');
CALL add_transaction_entry_indexes('202610');
CALL add_transaction_entry_indexes('202611');
CALL add_transaction_entry_indexes('202612');

-- =====================================================
-- 账户表索引优化
-- =====================================================
ALTER TABLE t_account 
    ADD INDEX IF NOT EXISTS idx_user_type_status (user_id, account_type, status),
    ADD INDEX IF NOT EXISTS idx_create_time (create_time);

-- =====================================================
-- 账户冻结日志表索引优化
-- =====================================================
ALTER TABLE t_account_freeze_log_0 
    ADD INDEX IF NOT EXISTS idx_account_operate_time (account_id, operate_time);
ALTER TABLE t_account_freeze_log_1 
    ADD INDEX IF NOT EXISTS idx_account_operate_time (account_id, operate_time);
ALTER TABLE t_account_freeze_log_2 
    ADD INDEX IF NOT EXISTS idx_account_operate_time (account_id, operate_time);
ALTER TABLE t_account_freeze_log_3 
    ADD INDEX IF NOT EXISTS idx_account_operate_time (account_id, operate_time);

-- =====================================================
-- 账户缓冲记账流水表索引优化
-- =====================================================
ALTER TABLE t_account_buffer_log 
    ADD INDEX IF NOT EXISTS idx_account_status_create (account_id, status, create_time),
    ADD INDEX IF NOT EXISTS idx_status_create (status, create_time);

-- =====================================================
-- 账户影子分片表索引优化
-- =====================================================
ALTER TABLE t_account_shard 
    ADD INDEX IF NOT EXISTS idx_main_status (main_account_id, status),
    ADD INDEX IF NOT EXISTS idx_merge_status (merge_status, last_merge_time);

-- =====================================================
-- 数据库：core_ledger_1
-- =====================================================
USE core_ledger_1;

-- 同样为第二个库添加索引
CALL add_transaction_indexes('202501');
CALL add_transaction_indexes('202502');
CALL add_transaction_indexes('202503');
CALL add_transaction_indexes('202504');
CALL add_transaction_indexes('202505');
CALL add_transaction_indexes('202506');
CALL add_transaction_indexes('202507');
CALL add_transaction_indexes('202508');
CALL add_transaction_indexes('202509');
CALL add_transaction_indexes('202510');
CALL add_transaction_indexes('202511');
CALL add_transaction_indexes('202512');
CALL add_transaction_indexes('202601');
CALL add_transaction_indexes('202602');
CALL add_transaction_indexes('202603');
CALL add_transaction_indexes('202604');
CALL add_transaction_indexes('202605');
CALL add_transaction_indexes('202606');
CALL add_transaction_indexes('202607');
CALL add_transaction_indexes('202608');
CALL add_transaction_indexes('202609');
CALL add_transaction_indexes('202610');
CALL add_transaction_indexes('202611');
CALL add_transaction_indexes('202612');

CALL add_transaction_entry_indexes('202501');
CALL add_transaction_entry_indexes('202502');
CALL add_transaction_entry_indexes('202503');
CALL add_transaction_entry_indexes('202504');
CALL add_transaction_entry_indexes('202505');
CALL add_transaction_entry_indexes('202506');
CALL add_transaction_entry_indexes('202507');
CALL add_transaction_entry_indexes('202508');
CALL add_transaction_entry_indexes('202509');
CALL add_transaction_entry_indexes('202510');
CALL add_transaction_entry_indexes('202511');
CALL add_transaction_entry_indexes('202512');
CALL add_transaction_entry_indexes('202601');
CALL add_transaction_entry_indexes('202602');
CALL add_transaction_entry_indexes('202603');
CALL add_transaction_entry_indexes('202604');
CALL add_transaction_entry_indexes('202605');
CALL add_transaction_entry_indexes('202606');
CALL add_transaction_entry_indexes('202607');
CALL add_transaction_entry_indexes('202608');
CALL add_transaction_entry_indexes('202609');
CALL add_transaction_entry_indexes('202610');
CALL add_transaction_entry_indexes('202611');
CALL add_transaction_entry_indexes('202612');

ALTER TABLE t_account 
    ADD INDEX IF NOT EXISTS idx_user_type_status (user_id, account_type, status),
    ADD INDEX IF NOT EXISTS idx_create_time (create_time);

ALTER TABLE t_account_freeze_log_0 
    ADD INDEX IF NOT EXISTS idx_account_operate_time (account_id, operate_time);
ALTER TABLE t_account_freeze_log_1 
    ADD INDEX IF NOT EXISTS idx_account_operate_time (account_id, operate_time);
ALTER TABLE t_account_freeze_log_2 
    ADD INDEX IF NOT EXISTS idx_account_operate_time (account_id, operate_time);
ALTER TABLE t_account_freeze_log_3 
    ADD INDEX IF NOT EXISTS idx_account_operate_time (account_id, operate_time);

ALTER TABLE t_account_buffer_log 
    ADD INDEX IF NOT EXISTS idx_account_status_create (account_id, status, create_time),
    ADD INDEX IF NOT EXISTS idx_status_create (status, create_time);

ALTER TABLE t_account_shard 
    ADD INDEX IF NOT EXISTS idx_main_status (main_account_id, status),
    ADD INDEX IF NOT EXISTS idx_merge_status (merge_status, last_merge_time);

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_transaction_indexes;
DROP PROCEDURE IF EXISTS add_transaction_entry_indexes;
