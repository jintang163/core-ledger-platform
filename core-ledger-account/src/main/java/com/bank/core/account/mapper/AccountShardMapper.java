package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.core.account.entity.AccountShard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账户影子分片Mapper接口
 *
 * 核心功能：
 * 1. 影子分片的CRUD操作
 * 2. 分片余额的原子性更新
 * 3. 分片归并相关操作
 * 4. 按主账户查询分片列表
 *
 * 设计要点：
 * - 所有查询条件默认包含deleted = 0（逻辑删除过滤）
 * - 余额更新使用乐观锁（version字段）防止并发冲突
 * - 分片按shard_index升序排列，便于路由选择
 * - 活跃分片查询条件：status = 0（正常状态）
 */
@Mapper
public interface AccountShardMapper extends BaseMapper<AccountShard> {

    /**
     * 根据分片ID查询影子账户
     *
     * @param shardId 分片ID
     * @return 影子账户信息，不存在返回null
     */
    @Select("SELECT * FROM t_account_shard WHERE shard_id = #{shardId} AND deleted = 0")
    AccountShard selectByShardId(@Param("shardId") String shardId);

    /**
     * 根据主账户ID查询所有影子账户
     * 按分片索引升序排列
     *
     * @param mainAccountId 主账户ID
     * @return 影子账户列表（包含已关闭的）
     */
    @Select("SELECT * FROM t_account_shard WHERE main_account_id = #{mainAccountId} AND deleted = 0 ORDER BY shard_index ASC")
    List<AccountShard> selectByMainAccountId(@Param("mainAccountId") String mainAccountId);

    /**
     * 根据主账户ID查询所有活跃的影子账户
     * 查询条件：status = 0（正常状态）
     * 按分片索引升序排列，便于路由选择
     *
     * @param mainAccountId 主账户ID
     * @return 活跃的影子账户列表
     */
    @Select("SELECT * FROM t_account_shard WHERE main_account_id = #{mainAccountId} AND status = 0 AND deleted = 0 ORDER BY shard_index ASC")
    List<AccountShard> selectActiveShards(@Param("mainAccountId") String mainAccountId);

    /**
     * 增加影子账户余额（不带乐观锁）
     * 用于不需要并发控制的场景
     *
     * @param shardId 分片ID
     * @param amount 变动金额（分），正数增加，负数减少
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE t_account_shard SET balance = balance + #{amount}, update_time = #{updateTime} " +
            "WHERE shard_id = #{shardId} AND deleted = 0")
    int addBalance(@Param("shardId") String shardId,
                   @Param("amount") Long amount,
                   @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新影子账户余额（带乐观锁）
     * 使用版本号机制防止并发更新冲突
     * 更新条件：version = #{version}，更新成功后version自动+1
     *
     * 注意：如果返回0，表示版本号不匹配，需要重试
     *
     * @param shardId 分片ID
     * @param amount 变动金额（分），正数增加，负数减少
     * @param version 当前版本号
     * @param updateTime 更新时间
     * @return 影响行数，0表示并发冲突更新失败
     */
    @Update("UPDATE t_account_shard SET balance = balance + #{amount}, version = version + 1, update_time = #{updateTime} " +
            "WHERE shard_id = #{shardId} AND version = #{version} AND deleted = 0")
    int updateBalanceWithVersion(@Param("shardId") String shardId,
                                 @Param("amount") Long amount,
                                 @Param("version") Integer version,
                                 @Param("updateTime") LocalDateTime updateTime);

    /**
     * 归并后重置影子账户余额
     * 将余额置为0，更新归并时间和状态
     * 调用时机：影子账户余额已成功归并到主账户后
     *
     * @param shardId 分片ID
     * @param lastMergeTime 最后归并时间
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE t_account_shard SET balance = 0, last_merge_time = #{lastMergeTime}, " +
            "merge_status = 2, update_time = #{updateTime} WHERE shard_id = #{shardId} AND deleted = 0")
    int resetBalanceAfterMerge(@Param("shardId") String shardId,
                               @Param("lastMergeTime") LocalDateTime lastMergeTime,
                               @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新影子账户归并状态
     * 状态流转：0-未归并 -> 1-归并中 -> 2-已归并
     *
     * @param shardId 分片ID
     * @param mergeStatus 归并状态
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE t_account_shard SET merge_status = #{mergeStatus}, update_time = #{updateTime} " +
            "WHERE shard_id = #{shardId} AND deleted = 0")
    int updateMergeStatus(@Param("shardId") String shardId,
                          @Param("mergeStatus") Integer mergeStatus,
                          @Param("updateTime") LocalDateTime updateTime);
}
