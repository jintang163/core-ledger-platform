package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.entity.TransferOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface TransferOrderMapper extends BaseMapper<TransferOrder> {

    @Select("SELECT * FROM t_transfer_order WHERE transfer_id = #{transferId} AND deleted = 0")
    TransferOrder selectByTransferId(@Param("transferId") String transferId);

    @Select("SELECT * FROM t_transfer_order WHERE business_no = #{businessNo} AND deleted = 0 LIMIT 1")
    TransferOrder selectByBusinessNo(@Param("businessNo") String businessNo);

    @Select("<script>" +
            "SELECT * FROM t_transfer_order WHERE deleted = 0 " +
            "<if test='fromAccountId != null'> AND from_account_id = #{fromAccountId} </if>" +
            "<if test='toAccountId != null'> AND to_account_id = #{toAccountId} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='startTime != null'> AND create_time >= #{startTime} </if>" +
            "<if test='endTime != null'> AND create_time &lt;= #{endTime} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    Page<TransferOrder> selectByCondition(Page<TransferOrder> page,
                                           @Param("fromAccountId") String fromAccountId,
                                           @Param("toAccountId") String toAccountId,
                                           @Param("status") Integer status,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    @Update("UPDATE t_transfer_order SET status = #{status}, transaction_id = #{transactionId}, " +
            "transfer_time = #{transferTime}, update_time = #{updateTime} " +
            "WHERE transfer_id = #{transferId} AND deleted = 0")
    int updateStatus(@Param("transferId") String transferId,
                     @Param("status") Integer status,
                     @Param("transactionId") String transactionId,
                     @Param("transferTime") LocalDateTime transferTime,
                     @Param("updateTime") LocalDateTime updateTime);
}
