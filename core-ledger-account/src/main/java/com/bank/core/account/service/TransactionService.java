package com.bank.core.account.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.api.dto.TransactionCreateDTO;
import com.bank.core.api.dto.TransactionQueryDTO;
import com.bank.core.api.vo.TransactionVO;

public interface TransactionService {

    TransactionVO createTransaction(TransactionCreateDTO dto);

    TransactionVO getTransaction(String transactionId);

    TransactionVO getTransactionByBusinessNo(String businessNo);

    Page<TransactionVO> queryTransactions(TransactionQueryDTO dto);
}
