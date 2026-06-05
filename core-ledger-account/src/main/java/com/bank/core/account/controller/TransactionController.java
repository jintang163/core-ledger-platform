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

@Slf4j
@RestController
@RequestMapping("/api/transaction")
@Api(tags = "账务核心-复式记账")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/create")
    @ApiOperation("复式记账")
    @Timed(value = "transaction.create.duration", description = "记账请求耗时")
    public Result<TransactionVO> createTransaction(@RequestBody @Valid TransactionCreateDTO dto) {
        log.info("接收到记账请求, businessNo: {}, transactionType: {}", dto.getBusinessNo(), dto.getTransactionType());
        TransactionVO vo = transactionService.createTransaction(dto);
        return Result.success(vo);
    }

    @GetMapping("/{transactionId}")
    @ApiOperation("查询交易详情")
    public Result<TransactionVO> getTransaction(@PathVariable String transactionId) {
        log.info("查询交易详情, transactionId: {}", transactionId);
        TransactionVO vo = transactionService.getTransaction(transactionId);
        return Result.success(vo);
    }

    @GetMapping("/business/{businessNo}")
    @ApiOperation("根据业务流水号查询交易")
    public Result<TransactionVO> getTransactionByBusinessNo(@PathVariable String businessNo) {
        log.info("根据业务流水号查询交易, businessNo: {}", businessNo);
        TransactionVO vo = transactionService.getTransactionByBusinessNo(businessNo);
        return Result.success(vo);
    }

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
