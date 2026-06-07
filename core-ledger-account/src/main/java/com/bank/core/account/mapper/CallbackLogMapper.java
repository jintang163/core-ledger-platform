package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.core.account.entity.CallbackLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface CallbackLogMapper extends BaseMapper<CallbackLog> {

    @Select("SELECT * FROM t_callback_log WHERE log_id = #{logId} AND deleted = 0")
    CallbackLog selectByLogId(@Param("logId") String logId);

    @Select("SELECT * FROM t_callback_log WHERE status IN ('PENDING', 'FAILED') " +
            "AND retry_count < max_retry_times AND next_retry_time < #{now} AND deleted = 0 " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    List<CallbackLog> selectPendingCallbacks(
            @Param("now") Long now,
            @Param("limit") Integer limit
    );

    @Update("UPDATE t_callback_log SET status = #{status}, retry_count = retry_count + 1, " +
            "next_retry_time = #{nextRetryTime}, error_msg = #{errorMsg}, " +
            "response_body = #{responseBody}, response_status = #{responseStatus}, " +
            "execute_time = #{executeTime}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("nextRetryTime") Long nextRetryTime,
            @Param("errorMsg") String errorMsg,
            @Param("responseBody") String responseBody,
            @Param("responseStatus") Integer responseStatus,
            @Param("executeTime") LocalDateTime executeTime,
            @Param("updateTime") LocalDateTime updateTime
    );

    @Update("UPDATE t_callback_log SET status = 'SUCCESS', retry_count = retry_count + 1, " +
            "response_body = #{responseBody}, response_status = #{responseStatus}, " +
            "execute_time = #{executeTime}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    int markAsSuccess(
            @Param("id") Long id,
            @Param("responseBody") String responseBody,
            @Param("responseStatus") Integer responseStatus,
            @Param("executeTime") LocalDateTime executeTime,
            @Param("updateTime") LocalDateTime updateTime
    );
}
