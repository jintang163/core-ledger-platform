package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.core.account.entity.BatchTransferItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量转账明细Mapper
 * 提供批量转账明细的数据库操作接口
 */
@Mapper
public interface BatchTransferItemMapper extends BaseMapper<BatchTransferItem> {

    /**
     * 根据明细ID查询批量转账明细
     * @param itemId 明细ID
     * @return 批量转账明细
     */
    @Select("SELECT * FROM t_batch_transfer_item WHERE item_id = #{itemId} AND deleted = 0")
    BatchTransferItem selectByItemId(@Param("itemId") String itemId);

    /**
     * 根据批次ID查询批量转账明细列表
     * @param batchId 批次ID
     * @return 明细列表，按创建时间升序排列
     */
    @Select("SELECT * FROM t_batch_transfer_item WHERE batch_id = #{batchId} AND deleted = 0 ORDER BY create_time ASC")
    List<BatchTransferItem> selectByBatchId(@Param("batchId") String batchId);

    /**
     * 根据转账ID查询批量转账明细
     * @param transferId 转账订单ID
     * @return 批量转账明细
     */
    @Select("SELECT * FROM t_batch_transfer_item WHERE transfer_id = #{transferId} AND deleted = 0 LIMIT 1")
    BatchTransferItem selectByTransferId(@Param("transferId") String transferId);

    /**
     * 更新批量转账明细状态
     * @param itemId 明细ID
     * @param status 状态
     * @param transactionId 交易ID
     * @param failReason 失败原因
     * @param finishTime 完成时间
     * @param updateTime 更新时间
     * @return 更新行数
     */
    @Update("UPDATE t_batch_transfer_item SET status = #{status}, transaction_id = #{transactionId}, " +
            "fail_reason = #{failReason}, finish_time = #{finishTime}, update_time = #{updateTime} " +
            "WHERE item_id = #{itemId} AND deleted = 0")
    int updateStatus(@Param("itemId") String itemId,
                     @Param("status") Integer status,
                     @Param("transactionId") String transactionId,
                     @Param("failReason") String failReason,
                     @Param("finishTime") LocalDateTime finishTime,
                     @Param("updateTime") LocalDateTime updateTime);
}
