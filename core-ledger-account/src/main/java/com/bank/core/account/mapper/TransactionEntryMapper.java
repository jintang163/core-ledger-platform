package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.core.account.entity.TransactionEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TransactionEntryMapper extends BaseMapper<TransactionEntry> {

    @Select("SELECT * FROM t_transaction_entry WHERE transaction_id = #{transactionId} ORDER BY id ASC")
    List<TransactionEntry> selectByTransactionId(@Param("transactionId") String transactionId);
}
