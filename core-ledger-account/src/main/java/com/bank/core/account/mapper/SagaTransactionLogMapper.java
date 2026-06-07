package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.core.account.entity.SagaTransactionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SagaTransactionLogMapper extends BaseMapper<SagaTransactionLog> {

    @Select("SELECT * FROM t_saga_transaction_log WHERE saga_id = #{sagaId} ORDER BY create_time ASC")
    List<SagaTransactionLog> selectBySagaId(@Param("sagaId") String sagaId);

    @Select("SELECT * FROM t_saga_transaction_log WHERE step_status IN (0, 2, 4) AND retry_count < 5 AND create_time < #{createTime} AND deleted = 0")
    List<SagaTransactionLog> selectPendingTransactions(@Param("createTime") java.time.LocalDateTime createTime);

    @Select("SELECT * FROM t_saga_transaction_log WHERE saga_id = #{sagaId} AND step_id = #{stepId} ORDER BY create_time DESC LIMIT 1")
    SagaTransactionLog selectLatestStepLog(@Param("sagaId") String sagaId, @Param("stepId") String stepId);
}
