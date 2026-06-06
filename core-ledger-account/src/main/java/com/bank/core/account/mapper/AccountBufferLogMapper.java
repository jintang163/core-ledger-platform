package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.core.account.entity.AccountBufferLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓冲记账流水Mapper
 */
@Mapper
public interface AccountBufferLogMapper extends BaseMapper<AccountBufferLog> {

    @Select("SELECT * FROM t_account_buffer_log WHERE buffer_id = #{bufferId} AND deleted = 0")
    AccountBufferLog selectByBufferId(@Param("bufferId") String bufferId);

    @Select("SELECT * FROM t_account_buffer_log WHERE request_id = #{requestId} AND deleted = 0 LIMIT 1")
    AccountBufferLog selectByRequestId(@Param("requestId") String requestId);

    @Select("SELECT * FROM t_account_buffer_log WHERE business_no = #{businessNo} AND deleted = 0 LIMIT 1")
    AccountBufferLog selectByBusinessNo(@Param("businessNo") String businessNo);

    @Select("SELECT * FROM t_account_buffer_log WHERE status = #{status} AND deleted = 0 " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    List<AccountBufferLog> selectPendingLogs(@Param("status") Integer status,
                                             @Param("limit") Integer limit);

    @Select("SELECT * FROM t_account_buffer_log WHERE account_id = #{accountId} AND status IN (0, 1) AND deleted = 0 " +
            "ORDER BY create_time ASC")
    List<AccountBufferLog> selectPendingByAccountId(@Param("accountId") String accountId);

    @Update("UPDATE t_account_buffer_log SET status = #{status}, batch_no = #{batchNo}, " +
            "retry_count = retry_count + 1, error_msg = #{errorMsg}, process_time = #{processTime}, " +
            "transaction_id = #{transactionId}, update_time = #{updateTime} " +
            "WHERE buffer_id = #{bufferId} AND deleted = 0")
    int updateStatus(@Param("bufferId") String bufferId,
                     @Param("status") Integer status,
                     @Param("batchNo") String batchNo,
                     @Param("errorMsg") String errorMsg,
                     @Param("processTime") LocalDateTime processTime,
                     @Param("transactionId") String transactionId,
                     @Param("updateTime") LocalDateTime updateTime);

    @Update("<script>" +
            "UPDATE t_account_buffer_log SET status = #{status}, batch_no = #{batchNo}, " +
            "update_time = #{updateTime} WHERE buffer_id IN " +
            "<foreach collection='bufferIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND deleted = 0" +
            "</script>")
    int batchUpdateStatus(@Param("bufferIds") List<String> bufferIds,
                          @Param("status") Integer status,
                          @Param("batchNo") String batchNo,
                          @Param("updateTime") LocalDateTime updateTime);

    @Select("SELECT COALESCE(SUM(amount_fen), 0) FROM t_account_buffer_log " +
            "WHERE account_id = #{accountId} AND status IN (0, 1) AND deleted = 0")
    Long sumPendingAmountByAccountId(@Param("accountId") String accountId);
}
