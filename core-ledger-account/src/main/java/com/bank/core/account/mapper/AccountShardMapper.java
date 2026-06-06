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
 * 账户影子分片Mapper
 */
@Mapper
public interface AccountShardMapper extends BaseMapper<AccountShard> {

    @Select("SELECT * FROM t_account_shard WHERE shard_id = #{shardId} AND deleted = 0")
    AccountShard selectByShardId(@Param("shardId") String shardId);

    @Select("SELECT * FROM t_account_shard WHERE main_account_id = #{mainAccountId} AND deleted = 0 ORDER BY shard_index ASC")
    List<AccountShard> selectByMainAccountId(@Param("mainAccountId") String mainAccountId);

    @Select("SELECT * FROM t_account_shard WHERE main_account_id = #{mainAccountId} AND status = 0 AND deleted = 0 ORDER BY shard_index ASC")
    List<AccountShard> selectActiveShards(@Param("mainAccountId") String mainAccountId);

    @Update("UPDATE t_account_shard SET balance = balance + #{amount}, update_time = #{updateTime} " +
            "WHERE shard_id = #{shardId} AND deleted = 0")
    int addBalance(@Param("shardId") String shardId,
                   @Param("amount") Long amount,
                   @Param("updateTime") LocalDateTime updateTime);

    @Update("UPDATE t_account_shard SET balance = balance + #{amount}, version = version + 1, update_time = #{updateTime} " +
            "WHERE shard_id = #{shardId} AND version = #{version} AND deleted = 0")
    int updateBalanceWithVersion(@Param("shardId") String shardId,
                                 @Param("amount") Long amount,
                                 @Param("version") Integer version,
                                 @Param("updateTime") LocalDateTime updateTime);

    @Update("UPDATE t_account_shard SET balance = 0, last_merge_time = #{lastMergeTime}, " +
            "merge_status = 2, update_time = #{updateTime} WHERE shard_id = #{shardId} AND deleted = 0")
    int resetBalanceAfterMerge(@Param("shardId") String shardId,
                               @Param("lastMergeTime") LocalDateTime lastMergeTime,
                               @Param("updateTime") LocalDateTime updateTime);

    @Update("UPDATE t_account_shard SET merge_status = #{mergeStatus}, update_time = #{updateTime} " +
            "WHERE shard_id = #{shardId} AND deleted = 0")
    int updateMergeStatus(@Param("shardId") String shardId,
                          @Param("mergeStatus") Integer mergeStatus,
                          @Param("updateTime") LocalDateTime updateTime);
}
