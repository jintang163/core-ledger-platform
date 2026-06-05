package com.bank.core.account.service.impl;

import com.bank.core.account.service.AccountService;
import com.bank.core.api.dto.AccountCloseDTO;
import com.bank.core.api.dto.AccountCreateDTO;
import com.bank.core.api.dto.AccountFreezeDTO;
import com.bank.core.api.dto.AccountUnfreezeDTO;
import com.bank.core.api.service.AccountDubboService;
import com.bank.core.api.vo.AccountVO;
import com.bank.core.common.result.Result;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

@Slf4j
@DubboService(version = "1.0.0", group = "core-ledger", timeout = 5000)
@RequiredArgsConstructor
public class AccountDubboServiceImpl implements AccountDubboService {

    private final AccountService accountService;

    @Override
    @Timed(value = "account.create.duration", description = "创建账户耗时")
    public Result<AccountVO> createAccount(AccountCreateDTO dto) {
        try {
            AccountVO accountVO = accountService.createAccount(dto);
            return Result.success(accountVO);
        } catch (Exception e) {
            log.error("Dubbo创建账户异常", e);
            return Result.fail(e.getMessage());
        }
    }

    @Override
    @Timed(value = "account.get.duration", description = "查询账户耗时")
    public Result<AccountVO> getAccount(String accountId) {
        try {
            AccountVO accountVO = accountService.getAccount(accountId);
            return Result.success(accountVO);
        } catch (Exception e) {
            log.error("Dubbo查询账户异常", e);
            return Result.fail(e.getMessage());
        }
    }

    @Override
    @Timed(value = "account.getByNo.duration", description = "根据账号查询账户耗时")
    public Result<AccountVO> getAccountByNo(String accountNo) {
        try {
            AccountVO accountVO = accountService.getAccountByNo(accountNo);
            return Result.success(accountVO);
        } catch (Exception e) {
            log.error("Dubbo根据账号查询账户异常", e);
            return Result.fail(e.getMessage());
        }
    }

    @Override
    @Timed(value = "account.freeze.duration", description = "冻结账户耗时")
    public Result<AccountVO> freezeAccount(AccountFreezeDTO dto) {
        try {
            AccountVO accountVO = accountService.freezeAccount(dto);
            return Result.success(accountVO);
        } catch (Exception e) {
            log.error("Dubbo冻结账户异常", e);
            return Result.fail(e.getMessage());
        }
    }

    @Override
    @Timed(value = "account.unfreeze.duration", description = "解冻账户耗时")
    public Result<AccountVO> unfreezeAccount(AccountUnfreezeDTO dto) {
        try {
            AccountVO accountVO = accountService.unfreezeAccount(dto);
            return Result.success(accountVO);
        } catch (Exception e) {
            log.error("Dubbo解冻账户异常", e);
            return Result.fail(e.getMessage());
        }
    }

    @Override
    @Timed(value = "account.close.duration", description = "销户耗时")
    public Result<Void> closeAccount(AccountCloseDTO dto) {
        try {
            accountService.closeAccount(dto);
            return Result.success();
        } catch (Exception e) {
            log.error("Dubbo销户异常", e);
            return Result.fail(e.getMessage());
        }
    }
}
