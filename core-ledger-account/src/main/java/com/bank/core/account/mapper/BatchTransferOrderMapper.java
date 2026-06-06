package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.entity.BatchTransferOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface BatchTransferOrderMapper extends BaseMapper<BatchTransferOrder> {

    @Select("SELECT * FROM t_batch_transfer_order WHERE batch_id = #{batchId} AND deleted = 0")
    BatchTransferOrder selectByBatchId(@Param("batchId") String batchId);

    @Select("SELECT * FROM t_batch_transfer_order WHERE business_no = #{businessNo} AND deleted = 0 LIMIT 1")
    BatchTransferOrder selectByBusinessNo(@Param("businessNo") String businessNo);

    @Select("<script>" +
            "SELECT * FROM t_batch_transfer_order WHERE deleted = 0 " +
            "<if test='fromAccountId != null'> AND from_account_id = #{fromAccountId} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='startTime != null'> AND create_time >= #{startTime} </if>" +
            "<if test='endTime != null'> AND create_time &lt;= #{endTime} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    Page<BatchTransferOrder> selectByCondition(Page<BatchTransferOrder> page,
                                                @Param("fromAccountId") String fromAccountId,
                                                @Param("status") Integer status,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    @Update("UPDATE t_batch_transfer_order SET status = #{status}, " +
            "success_count = #{successCount}, success_amount = #{successAmount}, " +
            "fail_count = #{failCount}, fail_amount = #{failAmount}, " +
            "finish_time = #{finishTime}, update_time = #{updateTime} " +
            "WHERE batch_id = #{batchId} AND deleted = 0")
    int updateStatus(@Param("batchId") String batchId,
                     @Param("status") Integer status,
                     @Param("successCount") Integer successCount,
                     @Param("successAmount") BigDecimal successAmount,
                     @Param("failCount") Integer failCount,
                     @Param("failAmount") BigDecimal failAmount,
                     @Param("finishTime") LocalDateTime finishTime,
                     @Param("updateTime") LocalDateTime updateTime);
}
