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

/**
 * 账户管理Controller
 *
 * 核心功能：
 * 1. 账户创建：支持创建不同类型、不同币种的账户
 * 2. 账户查询：根据账户ID、账号查询账户信息
 * 3. 账户冻结：支持司法冻结、业务冻结等多种冻结类型
 * 4. 账户解冻：解除账户冻结状态
 * 5. 账户销户：关闭账户，余额清零
 *
 * 设计要点：
 * - 使用@Validated和@Valid注解启用参数校验
 * - 使用@Timed和@Counted注解进行接口性能监控
 * - 使用@Slf4j记录请求日志，便于问题排查
 * - 统一返回Result格式，便于前端处理
 * - 使用@RequiredArgsConstructor注入依赖，减少样板代码
 *
 * 接口路径：/api/account/*
 */
@Slf4j
@Api(tags = "账户管理")
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Validated
public class AccountController {

    /** 账户服务接口 */
    private final AccountService accountService;

    /**
     * 创建账户
     *
     * 业务逻辑：
     * 1. 校验用户是否已存在同类型同币种的账户
     * 2. 生成唯一的账户ID和账号
     * 3. 初始化账户余额为0，状态为正常
     * 4. 记录开户时间
     *
     * @param dto 账户创建请求参数
     * @return 创建成功的账户信息
     */
    @PostMapping("/create")
    @ApiOperation("创建账户")
    @Timed(value = "account.create.http.duration", description = "HTTP创建账户耗时")
    @Counted(value = "account.create.http.count", description = "HTTP创建账户次数")
    public Result<AccountVO> createAccount(@RequestBody @Valid AccountCreateDTO dto) {
        log.info("HTTP创建账户, userId: {}, accountType: {}", dto.getUserId(), dto.getAccountType());
        AccountVO accountVO = accountService.createAccount(dto);
        return Result.success(accountVO);
    }

    /**
     * 根据账户ID查询账户信息
     *
     * @param accountId 账户ID
     * @return 账户详情
     */
    @GetMapping("/{accountId}")
    @ApiOperation("查询账户信息")
    @Timed(value = "account.get.http.duration", description = "HTTP查询账户耗时")
    @Counted(value = "account.get.http.count", description = "HTTP查询账户次数")
    public Result<AccountVO> getAccount(@PathVariable String accountId) {
        log.info("HTTP查询账户, accountId: {}", accountId);
        AccountVO accountVO = accountService.getAccount(accountId);
        return Result.success(accountVO);
    }

    /**
     * 根据账号查询账户
     *
     * @param accountNo 账号
     * @return 账户详情
     */
    @GetMapping("/no/{accountNo}")
    @ApiOperation("根据账号查询账户")
    @Timed(value = "account.getByNo.http.duration", description = "HTTP根据账号查询账户耗时")
    public Result<AccountVO> getAccountByNo(@PathVariable String accountNo) {
        log.info("HTTP根据账号查询账户, accountNo: {}", accountNo);
        AccountVO accountVO = accountService.getAccountByNo(accountNo);
        return Result.success(accountVO);
    }

    /**
     * 冻结账户
     *
     * 业务逻辑：
     * 1. 校验账户状态是否为正常
     * 2. 更新账户状态为冻结
     * 3. 记录冻结类型、备注、时间、操作员
     * 4. 冻结后账户无法进行出金操作
     *
     * @param dto 冻结请求参数
     * @return 冻结后的账户信息
     */
    @PostMapping("/freeze")
    @ApiOperation("冻结账户")
    @Timed(value = "account.freeze.http.duration", description = "HTTP冻结账户耗时")
    @Counted(value = "account.freeze.http.count", description = "HTTP冻结账户次数")
    public Result<AccountVO> freezeAccount(@RequestBody @Valid AccountFreezeDTO dto) {
        log.info("HTTP冻结账户, accountId: {}, freezeType: {}", dto.getAccountId(), dto.getFreezeType());
        AccountVO accountVO = accountService.freezeAccount(dto);
        return Result.success(accountVO);
    }

    /**
     * 解冻账户
     *
     * 业务逻辑：
     * 1. 校验账户状态是否为冻结
     * 2. 更新账户状态为正常
     * 3. 清空冻结相关字段
     * 4. 解冻后账户恢复正常操作
     *
     * @param dto 解冻请求参数
     * @return 解冻后的账户信息
     */
    @PostMapping("/unfreeze")
    @ApiOperation("解冻账户")
    @Timed(value = "account.unfreeze.http.duration", description = "HTTP解冻账户耗时")
    @Counted(value = "account.unfreeze.http.count", description = "HTTP解冻账户次数")
    public Result<AccountVO> unfreezeAccount(@RequestBody @Valid AccountUnfreezeDTO dto) {
        log.info("HTTP解冻账户, accountId: {}, freezeType: {}", dto.getAccountId(), dto.getFreezeType());
        AccountVO accountVO = accountService.unfreezeAccount(dto);
        return Result.success(accountVO);
    }

    /**
     * 账户销户
     *
     * 业务逻辑：
     * 1. 校验账户状态是否为正常
     * 2. 校验账户余额是否为0（如需先结转余额）
     * 3. 将余额置为0，状态更新为已销户
     * 4. 记录销户时间
     * 5. 销户后账户无法进行任何操作
     *
     * @param dto 销户请求参数
     * @return 操作结果
     */
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
