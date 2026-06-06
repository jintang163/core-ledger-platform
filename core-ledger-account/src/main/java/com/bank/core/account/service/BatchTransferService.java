package com.bank.core.account.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.api.dto.BatchTransferDTO;
import com.bank.core.api.dto.BatchTransferQueryDTO;
import com.bank.core.api.vo.BatchTransferOrderVO;
import com.bank.core.api.vo.BatchTransferItemVO;

import java.util.List;

public interface BatchTransferService {

    BatchTransferOrderVO batchTransfer(BatchTransferDTO dto);

    BatchTransferOrderVO getBatchTransferOrder(String batchId);

    BatchTransferOrderVO getBatchTransferOrderByBusinessNo(String businessNo);

    List<BatchTransferItemVO> getBatchTransferItems(String batchId);

    Page<BatchTransferOrderVO> queryBatchTransferOrders(BatchTransferQueryDTO dto);
}
