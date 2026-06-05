package com.bank.core.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.core.account.entity.AccountFreezeLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountFreezeLogMapper extends BaseMapper<AccountFreezeLog> {
}
