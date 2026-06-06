package com.bank.core.account.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.service.TransactionService;
import com.bank.core.api.dto.TransactionCreateDTO;
import com.bank.core.api.dto.TransactionQueryDTO;
import com.bank.core.api.vo.TransactionVO;
import com.bank.core.common.result.Result;
import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 复式记账Controller
 *
 * 核心功能：
 * 1. 复式记账：创建包含借贷分录的交易记录，确保借贷平衡
 * 2. 交易查询：根据交易ID、业务流水号查询交易详情
 * 3. 交易列表：多维度分页查询交易明细
 *
 * 设计要点：
 * - 支持借贷分录的复式记账，确保账务平衡
 * - 热点账户自动路由到影子账户或使用缓冲记账
 * - 使用分布式锁防止同一账户并发更新
 * - 支持乐观锁重试机制处理并发冲突
 * - 统一返回Result格式，便于前端处理
 *
 * 业务场景：
 * - 账户间转账（一借一贷）
 * - 手续费收取（一借多贷）
 * - 利息计提（多借多贷）
 * - 调账处理（红字冲销）
 *
 * 接口路径：/api/transaction/*
 */
@Slf4j
@RestController
@RequestMapping("/api/transaction")
@Api(tags = "账务核心-复式记账")
@RequiredArgsConstructor
public class TransactionController {

    /** 交易服务接口 */
    private final TransactionService transactionService;

    /**
     * 创建交易（复式记账）
     *
     * 业务流程：
     * 1. 校验参数合法性和借贷平衡性
     * 2. 幂等性校验（通过requestId和businessNo）
     * 3. 获取所有涉及账户的分布式锁（按ID排序防止死锁）
     * 4. 检查账户状态和余额（扣款账户需检查余额是否充足）
     * 5. 判断账户是否为热点账户，决定是否使用分片或缓冲
     * 6. 逐笔更新账户余额（使用乐观锁+重试机制）
     * 7. 记录交易和分录明细
     *
     * 注意事项：
     * - 所有分录的借方金额合计必须等于贷方金额合计
     * - 扣款账户余额必须充足（允许透支的账户除外）
     * - 热点账户会自动使用分片或缓冲记账提高并发性能
     *
     * @param dto 交易创建请求参数
     * @return 交易创建结果，包含交易ID和状态
     */
    @PostMapping("/create")
    @ApiOperation("复式记账")
    @Timed(value = "transaction.create.duration", description = "记账请求耗时")
    public Result<TransactionVO> createTransaction(@RequestBody @Valid TransactionCreateDTO dto) {
        log.info("接收到记账请求, businessNo: {}, transactionType: {}", dto.getBusinessNo(), dto.getTransactionType());
        TransactionVO vo = transactionService.createTransaction(dto);
        return Result.success(vo);
    }

    /**
     * 根据交易ID查询交易详情
     *
     * @param transactionId 交易ID
     * @return 交易详情（包含所有分录信息）
     */
    @GetMapping("/{transactionId}")
    @ApiOperation("查询交易详情")
    public Result<TransactionVO> getTransaction(@PathVariable String transactionId) {
        log.info("查询交易详情, transactionId: {}", transactionId);
        TransactionVO vo = transactionService.getTransaction(transactionId);
        return Result.success(vo);
    }

    /**
     * 根据业务流水号查询交易
     *
     * @param businessNo 业务流水号（调用方传入）
     * @return 交易详情
     */
    @GetMapping("/business/{businessNo}")
    @ApiOperation("根据业务流水号查询交易")
    public Result<TransactionVO> getTransactionByBusinessNo(@PathVariable String businessNo) {
        log.info("根据业务流水号查询交易, businessNo: {}", businessNo);
        TransactionVO vo = transactionService.getTransactionByBusinessNo(businessNo);
        return Result.success(vo);
    }

    /**
     * 分页查询交易列表
     *
     * 支持的查询维度：
     * - 账户ID：查询该账户相关的所有交易
     * - 交易类型：按交易类型过滤
     * - 交易状态：按状态过滤
     * - 时间范围：按交易时间范围过滤
     *
     * @param dto 查询条件（包含分页参数）
     * @return 分页的交易列表
     */
    @PostMapping("/query")
    @ApiOperation("分页查询交易明细")
    @Timed(value = "transaction.query.duration", description = "查询交易列表耗时")
    public Result<Page<TransactionVO>> queryTransactions(@RequestBody TransactionQueryDTO dto) {
        log.info("查询交易列表, accountId: {}, type: {}, status: {}",
                dto.getAccountId(), dto.getTransactionType(), dto.getStatus());
        Page<TransactionVO> page = transactionService.queryTransactions(dto);
        return Result.success(page);
    }
}
