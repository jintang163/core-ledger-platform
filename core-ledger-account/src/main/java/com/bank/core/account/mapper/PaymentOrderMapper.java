package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    @Select("SELECT * FROM t_payment_order WHERE payment_id = #{paymentId} AND deleted = 0")
    PaymentOrder selectByPaymentId(@Param("paymentId") String paymentId);

    @Select("SELECT * FROM t_payment_order WHERE business_no = #{businessNo} AND deleted = 0 LIMIT 1")
    PaymentOrder selectByBusinessNo(@Param("businessNo") String businessNo);

    @Select("<script>" +
            "SELECT * FROM t_payment_order WHERE deleted = 0 " +
            "<if test='accountId != null'> AND account_id = #{accountId} </if>" +
            "<if test='paymentType != null'> AND payment_type = #{paymentType} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='channelCode != null'> AND channel_code = #{channelCode} </if>" +
            "<if test='startTime != null'> AND create_time >= #{startTime} </if>" +
            "<if test='endTime != null'> AND create_time &lt;= #{endTime} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    Page<PaymentOrder> selectByCondition(Page<PaymentOrder> page,
                                          @Param("accountId") String accountId,
                                          @Param("paymentType") Integer paymentType,
                                          @Param("status") Integer status,
                                          @Param("channelCode") String channelCode,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    @Update("UPDATE t_payment_order SET status = #{status}, channel_status = #{channelStatus}, " +
            "channel_order_no = #{channelOrderNo}, channel_time = #{channelTime}, " +
            "success_time = #{successTime}, update_time = #{updateTime} " +
            "WHERE payment_id = #{paymentId} AND deleted = 0")
    int updateStatus(@Param("paymentId") String paymentId,
                     @Param("status") Integer status,
                     @Param("channelStatus") Integer channelStatus,
                     @Param("channelOrderNo") String channelOrderNo,
                     @Param("channelTime") LocalDateTime channelTime,
                     @Param("successTime") LocalDateTime successTime,
                     @Param("updateTime") LocalDateTime updateTime);
}
