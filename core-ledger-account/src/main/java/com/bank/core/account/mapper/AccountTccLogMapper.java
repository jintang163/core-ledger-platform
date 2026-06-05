package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.core.account.entity.AccountTccLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AccountTccLogMapper extends BaseMapper<AccountTccLog> {

    @Select("SELECT * FROM t_account_tcc_log WHERE tx_id = #{txId} AND action_name = #{actionName} AND phase = #{phase} LIMIT 1")
    AccountTccLog selectByTxIdAndActionAndPhase(
            @Param("txId") String txId,
            @Param("actionName") String actionName,
            @Param("phase") Integer phase
    );
}
