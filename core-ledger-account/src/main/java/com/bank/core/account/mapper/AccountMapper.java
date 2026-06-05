package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.core.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    @Select("SELECT * FROM t_account WHERE account_id = #{accountId} AND deleted = 0")
    Account selectByAccountId(@Param("accountId") String accountId);

    @Select("SELECT * FROM t_account WHERE account_no = #{accountNo} AND deleted = 0")
    Account selectByAccountNo(@Param("accountNo") String accountNo);

    @Select("SELECT * FROM t_account WHERE user_id = #{userId} AND account_type = #{accountType} " +
            "AND currency = #{currency} AND deleted = 0 LIMIT 1")
    Account selectByUserIdAndTypeAndCurrency(
            @Param("userId") String userId,
            @Param("accountType") Integer accountType,
            @Param("currency") String currency
    );

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

    @Update("UPDATE t_account SET status = #{status}, freeze_type = NULL, " +
            "freeze_remark = NULL, freeze_time = NULL, freeze_operator = NULL, " +
            "update_time = #{updateTime} WHERE account_id = #{accountId} AND deleted = 0")
    int unfreezeAccount(
            @Param("accountId") String accountId,
            @Param("status") Integer status,
            @Param("updateTime") LocalDateTime updateTime
    );

    @Update("UPDATE t_account SET balance = 0, status = #{status}, " +
            "update_time = #{updateTime} WHERE account_id = #{accountId} AND deleted = 0")
    int closeAccount(
            @Param("accountId") String accountId,
            @Param("status") Integer status,
            @Param("updateTime") LocalDateTime updateTime
    );

    @Update("UPDATE t_account SET balance = balance + #{amount}, " +
            "update_time = #{updateTime} WHERE account_id = #{accountId} AND deleted = 0")
    int addBalance(
            @Param("accountId") String accountId,
            @Param("amount") BigDecimal amount,
            @Param("updateTime") LocalDateTime updateTime
    );
}
