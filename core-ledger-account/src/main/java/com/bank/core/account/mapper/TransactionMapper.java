package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface TransactionMapper extends BaseMapper<Transaction> {

    @Select("SELECT * FROM t_transaction WHERE transaction_id = #{transactionId} AND deleted = 0")
    Transaction selectByTransactionId(@Param("transactionId") String transactionId);

    @Select("SELECT * FROM t_transaction WHERE business_no = #{businessNo} AND deleted = 0 LIMIT 1")
    Transaction selectByBusinessNo(@Param("businessNo") String businessNo);

    @Select("<script>" +
            "SELECT * FROM t_transaction WHERE deleted = 0 " +
            "<if test='accountId != null'>" +
            "AND id IN (SELECT DISTINCT t.id FROM t_transaction t " +
            "INNER JOIN t_transaction_entry e ON t.transaction_id = e.transaction_id " +
            "WHERE e.account_id = #{accountId})" +
            "</if>" +
            "<if test='transactionType != null'> AND transaction_type = #{transactionType} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='startTime != null'> AND transaction_time >= #{startTime} </if>" +
            "<if test='endTime != null'> AND transaction_time &lt;= #{endTime} </if>" +
            "ORDER BY transaction_time DESC" +
            "</script>")
    Page<Transaction> selectByCondition(Page<Transaction> page,
                                         @Param("accountId") String accountId,
                                         @Param("transactionType") Integer transactionType,
                                         @Param("status") Integer status,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
}
