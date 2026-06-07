package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.core.account.entity.ReliableMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface ReliableMessageMapper extends BaseMapper<ReliableMessage> {

    @Select("SELECT * FROM t_reliable_message WHERE message_id = #{messageId} AND deleted = 0")
    ReliableMessage selectByMessageId(@Param("messageId") String messageId);

    @Select("SELECT * FROM t_reliable_message WHERE status IN ('PENDING', 'FAILED') " +
            "AND retry_count < max_retry_times AND next_retry_time < #{now} AND deleted = 0 " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    List<ReliableMessage> selectPendingMessages(
            @Param("now") Long now,
            @Param("limit") Integer limit
    );

    @Update("UPDATE t_reliable_message SET status = #{status}, retry_count = retry_count + 1, " +
            "next_retry_time = #{nextRetryTime}, error_msg = #{errorMsg}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("nextRetryTime") Long nextRetryTime,
            @Param("errorMsg") String errorMsg,
            @Param("updateTime") LocalDateTime updateTime
    );

    @Update("UPDATE t_reliable_message SET status = 'SUCCESS', send_time = #{sendTime}, " +
            "update_time = #{updateTime} WHERE id = #{id}")
    int markAsSuccess(
            @Param("id") Long id,
            @Param("sendTime") LocalDateTime sendTime,
            @Param("updateTime") LocalDateTime updateTime
    );
}
