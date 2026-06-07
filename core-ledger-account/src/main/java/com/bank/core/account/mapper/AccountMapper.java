package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.core.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账户Mapper接口
 *
 * 核心功能：
 * 1. 账户的CRUD操作（继承BaseMapper）
 * 2. 账户余额的原子性更新
 * 3. 账户状态变更（冻结、解冻、销户）
 * 4. 热点账户查询
 *
 * 设计要点：
 * - 所有查询条件默认包含deleted = 0（逻辑删除过滤）
 * - 余额更新使用乐观锁（version字段）防止并发冲突
 * - 使用@Param注解明确参数名称，避免参数绑定错误
 * - 热点账户查询条件：hot_status IN (2, 3) 表示已分片或已启用缓冲
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    /**
     * 根据账户ID查询账户
     *
     * @param accountId 账户ID（业务主键）
     * @return 账户信息，不存在返回null
     */
    @Select("SELECT * FROM t_account WHERE account_id = #{accountId} AND deleted = 0")
    Account selectByAccountId(@Param("accountId") String accountId);

    /**
     * 根据账号查询账户
     *
     * @param accountNo 账号（展示给用户的编号）
     * @return 账户信息，不存在返回null
     */
    @Select("SELECT * FROM t_account WHERE account_no = #{accountNo} AND deleted = 0")
    Account selectByAccountNo(@Param("accountNo") String accountNo);

    /**
     * 根据用户ID、账户类型、币种查询账户
     * 用于确保同一用户同一类型同一币种只有一个账户
     *
     * @param userId 用户ID
     * @param accountType 账户类型
     * @param currency 币种
     * @return 账户信息，不存在返回null
     */
    @Select("SELECT * FROM t_account WHERE user_id = #{userId} AND account_type = #{accountType} " +
            "AND currency = #{currency} AND deleted = 0 LIMIT 1")
    Account selectByUserIdAndTypeAndCurrency(
            @Param("userId") String userId,
            @Param("accountType") Integer accountType,
            @Param("currency") String currency
    );

    /**
     * 冻结账户
     * 更新账户状态为冻结，并记录冻结相关信息
     *
     * @param accountId 账户ID
     * @param status 账户状态（1-冻结）
     * @param freezeType 冻结类型
     * @param freezeRemark 冻结备注
     * @param freezeTime 冻结时间
     * @param freezeOperator 冻结操作员
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE t_account SET status = #{status}, freeze_type = #{freezeType}, " +
            "freeze_remark = #{freezeRemark}, freeze_time = #{freezeTime}, " +
            "freeze_operator = #{freezeOperator}, update_time = #{updateTime} " +
            "WHERE account_id = #{accountId} AND deleted = 0")
    int freezeAccount(
            @Param("accountId") String accountId,
            @Param("status") Integer status,
            @Param("freezeType") Integer freezeType,
            @Param("freezeRemark") String freezeRemark,
            @Param("freezeTime") LocalDateTime freezeTime,
            @Param("freezeOperator") String freezeOperator,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 解冻账户
     * 更新账户状态为正常，清空冻结相关信息
     *
     * @param accountId 账户ID
     * @param status 账户状态（0-正常）
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE t_account SET status = #{status}, freeze_type = NULL, " +
            "freeze_remark = NULL, freeze_time = NULL, freeze_operator = NULL, " +
            "update_time = #{updateTime} WHERE account_id = #{accountId} AND deleted = 0")
    int unfreezeAccount(
            @Param("accountId") String accountId,
            @Param("status") Integer status,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 账户销户
     * 将余额置为0，更新状态为已销户，记录销户时间
     *
     * @param accountId 账户ID
     * @param status 账户状态（2-已销户）
     * @param closeTime 销户时间
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE t_account SET balance = 0, status = #{status}, " +
            "freeze_type = NULL, freeze_remark = NULL, freeze_time = NULL, freeze_operator = NULL, " +
            "close_time = #{closeTime}, update_time = #{updateTime} " +
            "WHERE account_id = #{accountId} AND deleted = 0")
    int closeAccount(
            @Param("accountId") String accountId,
            @Param("status") Integer status,
            @Param("closeTime") LocalDateTime closeTime,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 增加账户余额（不带乐观锁）
     * 用于不需要并发控制的场景
     *
     * @param accountId 账户ID
     * @param amount 变动金额（分），正数增加，负数减少
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE t_account SET balance = balance + #{amount}, " +
            "update_time = #{updateTime} WHERE account_id = #{accountId} AND deleted = 0")
    int addBalance(
            @Param("accountId") String accountId,
            @Param("amount") Long amount,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 更新账户余额（带乐观锁）
     * 使用版本号机制防止并发更新冲突
     * 更新条件：version = #{version}，更新成功后version自动+1
     *
     * 注意：如果返回0，表示版本号不匹配，需要重试
     *
     * @param accountId 账户ID
     * @param amount 变动金额（分），正数增加，负数减少
     * @param version 当前版本号
     * @param updateTime 更新时间
     * @return 影响行数，0表示并发冲突更新失败
     */
    @Update("UPDATE t_account SET balance = balance + #{amount}, version = version + 1, " +
            "update_time = #{updateTime} WHERE account_id = #{accountId} AND version = #{version} AND deleted = 0")
    int updateBalanceWithVersion(
            @Param("accountId") String accountId,
            @Param("amount") Long amount,
            @Param("version") Integer version,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 查询所有热点账户
     * 用于定时任务归并影子账户余额
     * 查询条件：hot_status IN (2, 3) 表示已分片或已启用缓冲
     *
     * @return 热点账户列表
     */
    @Select("SELECT * FROM t_account WHERE hot_status IN (2, 3) AND deleted = 0")
    List<Account> selectAllHotAccounts();

    /**
     * 统计余额为负数的账户数量
     * 用于监控告警
     *
     * @return 负数余额账户数量
     */
    @Select("SELECT COUNT(*) FROM t_account WHERE balance < 0 AND deleted = 0")
    long countNegativeBalanceAccounts();

    /**
     * 查询异常冻结的账户
     * 冻结时间超过指定阈值的账户
     *
     * @param maxFreezeHours 最大冻结小时数
     * @return 异常冻结账户列表
     */
    @Select("SELECT * FROM t_account WHERE status = 1 AND freeze_time < #{freezeTime} AND deleted = 0")
    List<Account> selectAbnormalFrozenAccounts(@Param("freezeTime") LocalDateTime freezeTime);
}
