package com.bank.core.account.controller;

import com.bank.core.account.service.HotAccountService;
import com.bank.core.api.dto.HotAccountConfigDTO;
import com.bank.core.api.vo.AccountShardVO;
import com.bank.core.api.vo.HotAccountConfigVO;
import com.bank.core.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = "热点账户管理")
@RestController
@RequestMapping("/api/hot-account")
@RequiredArgsConstructor
public class HotAccountController {

    private final HotAccountService hotAccountService;

    @PostMapping("/mark")
    @ApiOperation("标记账户为热点账户")
    public Result<HotAccountConfigVO> markAsHotAccount(@RequestBody HotAccountConfigDTO dto) {
        log.info("标记热点账户, accountId: {}, shardCount: {}", dto.getAccountId(), dto.getShardCount());
        hotAccountService.markAsHotAccount(dto.getAccountId(), dto.getShardCount());
        return Result.success(getHotAccountConfigInternal(dto.getAccountId()));
    }

    @PostMapping("/unmark/{accountId}")
    @ApiOperation("取消热点账户标记")
    public Result<Void> unmarkAsHotAccount(@PathVariable String accountId) {
        log.info("取消热点账户标记, accountId: {}", accountId);
        hotAccountService.unmarkAsHotAccount(accountId);
        return Result.success();
    }

    @GetMapping("/config/{accountId}")
    @ApiOperation("获取热点账户配置")
    public Result<HotAccountConfigVO> getHotAccountConfig(@PathVariable String accountId) {
        log.info("获取热点账户配置, accountId: {}", accountId);
        return Result.success(getHotAccountConfigInternal(accountId));
    }

    @PostMapping("/config")
    @ApiOperation("更新热点账户配置")
    public Result<HotAccountConfigVO> updateHotAccountConfig(@RequestBody HotAccountConfigDTO dto) {
        log.info("更新热点账户配置, accountId: {}", dto.getAccountId());
        return Result.success(getHotAccountConfigInternal(dto.getAccountId()));
    }

    @PostMapping("/list")
    @ApiOperation("查询热点账户列表")
    public Result<Object> getHotAccountList(@RequestBody(required = false) HotAccountConfigDTO params) {
        log.info("查询热点账户列表");
        return Result.success(null);
    }

    @GetMapping("/shards/{mainAccountId}")
    @ApiOperation("查询账户分片列表")
    public Result<List<AccountShardVO>> getAccountShards(@PathVariable String mainAccountId) {
        log.info("查询账户分片列表, mainAccountId: {}", mainAccountId);
        return Result.success(null);
    }

    @PostMapping("/merge/{mainAccountId}")
    @ApiOperation("手动归并分片余额")
    public Result<Long> mergeShards(@PathVariable String mainAccountId) {
        log.info("手动归并分片余额, mainAccountId: {}", mainAccountId);
        Long mergedAmount = hotAccountService.mergeShards(mainAccountId);
        return Result.success(mergedAmount);
    }

    @PostMapping("/merge-all")
    @ApiOperation("归并所有热点账户分片")
    public Result<Void> mergeAllShards() {
        log.info("归并所有热点账户分片");
        hotAccountService.mergeAllShards();
        return Result.success();
    }

    private HotAccountConfigVO getHotAccountConfigInternal(String accountId) {
        HotAccountConfigVO vo = new HotAccountConfigVO();
        vo.setAccountId(accountId);
        vo.setIsHotAccount(hotAccountService.isHotAccount(accountId));
        vo.setShardCount(10);
        vo.setShardingStrategy(1);
        vo.setShardingStrategyDesc("哈希路由");
        vo.setBufferEnabled(true);
        vo.setBufferThreshold(1000);
        return vo;
    }
}
