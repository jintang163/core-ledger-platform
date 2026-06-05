package com.bank.core.account.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.service.TransactionService;
import com.bank.core.api.dto.TransactionCreateDTO;
import com.bank.core.api.dto.TransactionQueryDTO;
import com.bank.core.api.service.TransactionDubboService;
import com.bank.core.api.vo.TransactionVO;
import com.bank.core.common.result.Result;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

@Slf4j
@DubboService(group = "CORE_LEDGER_GROUP", version = "1.0.0")
@RequiredArgsConstructor
public class TransactionDubboServiceImpl implements TransactionDubboService {

    private final TransactionService transactionService;

    @Override
    @Timed(value = "transaction.create.duration", description = "记账请求耗时")
    public Result<TransactionVO> createTransaction(TransactionCreateDTO dto) {
        try {
            TransactionVO vo = transactionService.createTransaction(dto);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("记账失败, businessNo: {}", dto.getBusinessNo(), e);
            return Result.fail(null, e.getMessage());
        }
    }

    @Override
    @Timed(value = "transaction.get.duration", description = "查询交易耗时")
    public Result<TransactionVO> getTransaction(String transactionId) {
        try {
            TransactionVO vo = transactionService.getTransaction(transactionId);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("查询交易失败, transactionId: {}", transactionId, e);
            return Result.fail(null, e.getMessage());
        }
    }

    @Override
    public Result<TransactionVO> getTransactionByBusinessNo(String businessNo) {
        try {
            TransactionVO vo = transactionService.getTransactionByBusinessNo(businessNo);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("根据业务流水号查询交易失败, businessNo: {}", businessNo, e);
            return Result.fail(null, e.getMessage());
        }
    }

    @Override
    @Timed(value = "transaction.query.duration", description = "查询交易列表耗时")
    public Result<Page<TransactionVO>> queryTransactions(TransactionQueryDTO dto) {
        try {
            Page<TransactionVO> page = transactionService.queryTransactions(dto);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询交易列表失败", e);
            return Result.fail(null, e.getMessage());
        }
    }
}
