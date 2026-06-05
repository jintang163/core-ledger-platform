package com.bank.core.api.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.api.dto.TransactionCreateDTO;
import com.bank.core.api.dto.TransactionQueryDTO;
import com.bank.core.api.vo.TransactionVO;
import com.bank.core.common.result.Result;

public interface TransactionDubboService {

    Result<TransactionVO> createTransaction(TransactionCreateDTO dto);

    Result<TransactionVO> getTransaction(String transactionId);

    Result<TransactionVO> getTransactionByBusinessNo(String businessNo);

    Result<Page<TransactionVO>> queryTransactions(TransactionQueryDTO dto);
}
