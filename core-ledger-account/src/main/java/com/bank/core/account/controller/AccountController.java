package com.bank.core.account.controller;

import com.bank.core.account.service.AccountService;
import com.bank.core.api.dto.AccountCloseDTO;
import com.bank.core.api.dto.AccountCreateDTO;
import com.bank.core.api.dto.AccountFreezeDTO;
import com.bank.core.api.dto.AccountUnfreezeDTO;
import com.bank.core.api.vo.AccountVO;
import com.bank.core.common.result.Result;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@Api(tags = "账户管理")
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/create")
    @ApiOperation("创建账户")
    @Timed(value = "account.create.http.duration", description = "HTTP创建账户耗时")
    @Counted(value = "account.create.http.count", description = "HTTP创建账户次数")
    public Result<AccountVO> createAccount(@RequestBody @Valid AccountCreateDTO dto) {
        log.info("HTTP创建账户, userId: {}, accountType: {}", dto.getUserId(), dto.getAccountType());
        AccountVO accountVO = accountService.createAccount(dto);
        return Result.success(accountVO);
    }

    @GetMapping("/{accountId}")
    @ApiOperation("查询账户信息")
    @Timed(value = "account.get.http.duration", description = "HTTP查询账户耗时")
    @Counted(value = "account.get.http.count", description = "HTTP查询账户次数")
    public Result<AccountVO> getAccount(@PathVariable String accountId) {
        log.info("HTTP查询账户, accountId: {}", accountId);
        AccountVO accountVO = accountService.getAccount(accountId);
        return Result.success(accountVO);
    }

    @GetMapping("/no/{accountNo}")
    @ApiOperation("根据账号查询账户")
    @Timed(value = "account.getByNo.http.duration", description = "HTTP根据账号查询账户耗时")
    public Result<AccountVO> getAccountByNo(@PathVariable String accountNo) {
        log.info("HTTP根据账号查询账户, accountNo: {}", accountNo);
        AccountVO accountVO = accountService.getAccountByNo(accountNo);
        return Result.success(accountVO);
    }

    @PostMapping("/freeze")
    @ApiOperation("冻结账户")
    @Timed(value = "account.freeze.http.duration", description = "HTTP冻结账户耗时")
    @Counted(value = "account.freeze.http.count", description = "HTTP冻结账户次数")
    public Result<AccountVO> freezeAccount(@RequestBody @Valid AccountFreezeDTO dto) {
        log.info("HTTP冻结账户, accountId: {}, freezeType: {}", dto.getAccountId(), dto.getFreezeType());
        AccountVO accountVO = accountService.freezeAccount(dto);
        return Result.success(accountVO);
    }

    @PostMapping("/unfreeze")
    @ApiOperation("解冻账户")
    @Timed(value = "account.unfreeze.http.duration", description = "HTTP解冻账户耗时")
    @Counted(value = "account.unfreeze.http.count", description = "HTTP解冻账户次数")
    public Result<AccountVO> unfreezeAccount(@RequestBody @Valid AccountUnfreezeDTO dto) {
        log.info("HTTP解冻账户, accountId: {}, freezeType: {}", dto.getAccountId(), dto.getFreezeType());
        AccountVO accountVO = accountService.unfreezeAccount(dto);
        return Result.success(accountVO);
    }

    @PostMapping("/close")
    @ApiOperation("账户销户")
    @Timed(value = "account.close.http.duration", description = "HTTP销户耗时")
    @Counted(value = "account.close.http.count", description = "HTTP销户次数")
    public Result<Void> closeAccount(@RequestBody @Valid AccountCloseDTO dto) {
        log.info("HTTP销户, accountId: {}", dto.getAccountId());
        accountService.closeAccount(dto);
        return Result.success();
    }
}
