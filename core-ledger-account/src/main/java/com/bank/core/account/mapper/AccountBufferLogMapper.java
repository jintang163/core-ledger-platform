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
 * 缓冲记账流水Mapper接口
 *
 * 核心功能：
 * 1. 缓冲流水的CRUD操作
 * 2. 批量状态更新（用于定时任务批量处理）
 * 3. 待处理流水查询
 * 4. 账户待处理金额汇总
 *
 * 设计要点：
 * - 所有查询条件默认包含deleted = 0（逻辑删除过滤）
 * - 幂等查询：按requestId和businessNo查询防止重复处理
 * - 批量更新使用foreach标签，提高处理效率
 * - 待处理金额汇总使用COALESCE函数处理null值，确保返回0而非null
 * - 待处理流水按创建时间升序排列，保证先进先出
 */
@Mapper
public interface AccountBufferLogMapper extends BaseMapper<AccountBufferLog> {

    /**
     * 根据缓冲流水ID查询
     *
     * @param bufferId 缓冲流水ID
     * @return 缓冲流水信息，不存在返回null
     */
    @Select("SELECT * FROM t_account_buffer_log WHERE buffer_id = #{bufferId} AND deleted = 0")
    AccountBufferLog selectByBufferId(@Param("bufferId") String bufferId);

    /**
     * 根据请求ID查询（幂等校验用）
     * 用于防止重复请求
     *
     * @param requestId 请求ID
     * @return 缓冲流水信息，不存在返回null
     */
    @Select("SELECT * FROM t_account_buffer_log WHERE request_id = #{requestId} AND deleted = 0 LIMIT 1")
    AccountBufferLog selectByRequestId(@Param("requestId") String requestId);

    /**
     * 根据业务流水号查询（幂等校验用）
     * 用于防止重复请求
     *
     * @param businessNo 业务流水号
     * @return 缓冲流水信息，不存在返回null
     */
    @Select("SELECT * FROM t_account_buffer_log WHERE business_no = #{businessNo} AND deleted = 0 LIMIT 1")
    AccountBufferLog selectByBusinessNo(@Param("businessNo") String businessNo);

    /**
     * 查询待处理的缓冲流水列表
     * 按创建时间升序排列，保证先进先出
     *
     * @param status 状态（0-待处理）
     * @param limit 最大返回数量
     * @return 待处理缓冲流水列表
     */
    @Select("SELECT * FROM t_account_buffer_log WHERE status = #{status} AND deleted = 0 " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    List<AccountBufferLog> selectPendingLogs(@Param("status") Integer status,
                                             @Param("limit") Integer limit);

    /**
     * 查询账户的待处理和处理中缓冲流水
     * 查询条件：status IN (0, 1) 表示待处理或处理中
     * 用于计算账户可用余额时需要考虑这些待处理金额
     *
     * @param accountId 账户ID
     * @return 待处理和处理中的缓冲流水列表
     */
    @Select("SELECT * FROM t_account_buffer_log WHERE account_id = #{accountId} AND status IN (0, 1) AND deleted = 0 " +
            "ORDER BY create_time ASC")
    List<AccountBufferLog> selectPendingByAccountId(@Param("accountId") String accountId);

    /**
     * 更新缓冲流水状态
     * 状态流转：待处理(0) -> 处理中(1) -> 成功(2)/失败(3)
     * 同时更新重试次数、错误信息、处理时间、关联交易ID等
     *
     * @param bufferId 缓冲流水ID
     * @param status 新状态
     * @param batchNo 处理批次号
     * @param errorMsg 错误信息（处理失败时填写）
     * @param processTime 处理完成时间
     * @param transactionId 关联的正式交易ID（处理成功后填写）
     * @param updateTime 更新时间
     * @return 影响行数
     */
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

    /**
     * 批量更新缓冲流水状态
     * 使用MyBatis的foreach标签批量更新，提高处理效率
     * 调用时机：定时任务批量处理前，将待处理状态更新为处理中
     *
     * @param bufferIds 缓冲流水ID列表
     * @param status 新状态
     * @param batchNo 处理批次号
     * @param updateTime 更新时间
     * @return 影响行数
     */
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

    /**
     * 汇总账户的待处理金额
     * 查询条件：status IN (0, 1) 表示待处理或处理中
     * 使用COALESCE函数处理null值，确保无记录时返回0而非null
     *
     * 计算公式：账户可用余额 = 账户实际余额 + 待处理缓冲金额
     *
     * @param accountId 账户ID
     * @return 待处理金额合计（分），正数表示待入账，负数表示待扣款
     */
    @Select("SELECT COALESCE(SUM(amount_fen), 0) FROM t_account_buffer_log " +
            "WHERE account_id = #{accountId} AND status IN (0, 1) AND deleted = 0")
    Long sumPendingAmountByAccountId(@Param("accountId") String accountId);
}
