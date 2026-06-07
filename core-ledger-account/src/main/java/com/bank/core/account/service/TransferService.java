package com.bank.core.account.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.api.dto.TransferDTO;
import com.bank.core.api.dto.TransferQueryDTO;
import com.bank.core.api.vo.TransferOrderVO;

public interface TransferService {

    TransferOrderVO transfer(TransferDTO dto);

    TransferOrderVO getTransferOrder(String transferId);

    TransferOrderVO getTransferOrderByBusinessNo(String businessNo);

    Page<TransferOrderVO> queryTransferOrders(TransferQueryDTO dto);

    TransferOrderVO crossBankTransfer(TransferDTO dto);
}
