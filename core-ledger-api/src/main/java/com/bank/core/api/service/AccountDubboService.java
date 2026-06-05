package com.bank.core.api.service;

import com.bank.core.api.dto.AccountCloseDTO;
import com.bank.core.api.dto.AccountCreateDTO;
import com.bank.core.api.dto.AccountFreezeDTO;
import com.bank.core.api.dto.AccountUnfreezeDTO;
import com.bank.core.api.vo.AccountVO;
import com.bank.core.common.result.Result;

public interface AccountDubboService {

    Result<AccountVO> createAccount(AccountCreateDTO dto);

    Result<AccountVO> getAccount(String accountId);

    Result<AccountVO> getAccountByNo(String accountNo);

    Result<AccountVO> freezeAccount(AccountFreezeDTO dto);

    Result<AccountVO> unfreezeAccount(AccountUnfreezeDTO dto);

    Result<Void> closeAccount(AccountCloseDTO dto);
}
