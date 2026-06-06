package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.core.account.entity.BatchTransferItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BatchTransferItemMapper extends BaseMapper<BatchTransferItem> {

    @Select("SELECT * FROM t_batch_transfer_item WHERE item_id = #{itemId}")
    BatchTransferItem selectByItemId(@Param("itemId") String itemId);

    @Select("SELECT * FROM t_batch_transfer_item WHERE batch_id = #{batchId} ORDER BY create_time ASC")
    List<BatchTransferItem> selectByBatchId(@Param("batchId") String batchId);

    @Select("SELECT * FROM t_batch_transfer_item WHERE transfer_id = #{transferId} LIMIT 1")
    BatchTransferItem selectByTransferId(@Param("transferId") String transferId);

    @Update("UPDATE t_batch_transfer_item SET status = #{status}, transaction_id = #{transactionId}, " +
            "fail_reason = #{failReason}, finish_time = #{finishTime}, update_time = #{updateTime} " +
            "WHERE item_id = #{itemId}")
    int updateStatus(@Param("itemId") String itemId,
                     @Param("status") Integer status,
                     @Param("transactionId") String transactionId,
                     @Param("failReason") String failReason,
                     @Param("finishTime") LocalDateTime finishTime,
                     @Param("updateTime") LocalDateTime updateTime);
}
