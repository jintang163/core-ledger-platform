package com.bank.core.account.service;

import com.bank.core.api.dto.AccountCloseDTO;
import com.bank.core.api.dto.AccountCreateDTO;
import com.bank.core.api.dto.AccountFreezeDTO;
import com.bank.core.api.dto.AccountUnfreezeDTO;
import com.bank.core.api.vo.AccountVO;

public interface AccountService {

    AccountVO createAccount(AccountCreateDTO dto);

    AccountVO getAccount(String accountId);

    AccountVO getAccountByNo(String accountNo);

    AccountVO freezeAccount(AccountFreezeDTO dto);

    AccountVO unfreezeAccount(AccountUnfreezeDTO dto);

    void closeAccount(AccountCloseDTO dto);
}
