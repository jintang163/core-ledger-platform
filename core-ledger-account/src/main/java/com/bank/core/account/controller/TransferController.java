package com.bank.core.account.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.service.TransferService;
import com.bank.core.api.dto.TransferDTO;
import com.bank.core.api.dto.TransferQueryDTO;
import com.bank.core.api.vo.TransferOrderVO;
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
@RequestMapping("/api/transfer")
@Api(tags = "支付核心-账户间转账")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/create")
    @ApiOperation("账户间转账")
    @Timed(value = "transfer.create.duration", description = "转账请求耗时")
    public Result<TransferOrderVO> transfer(@RequestBody @Valid TransferDTO dto) {
        log.info("接收到转账请求, businessNo: {}, fromAccountId: {}, toAccountId: {}, amount: {}",
                dto.getBusinessNo(), dto.getFromAccountId(), dto.getToAccountId(), dto.getAmount());
        TransferOrderVO vo = transferService.transfer(dto);
        return Result.success(vo);
    }

    @GetMapping("/{transferId}")
    @ApiOperation("查询转账订单详情")
    public Result<TransferOrderVO> getTransferOrder(@PathVariable String transferId) {
        log.info("查询转账订单详情, transferId: {}", transferId);
        TransferOrderVO vo = transferService.getTransferOrder(transferId);
        return Result.success(vo);
    }

    @GetMapping("/business/{businessNo}")
    @ApiOperation("根据业务流水号查询转账订单")
    public Result<TransferOrderVO> getTransferOrderByBusinessNo(@PathVariable String businessNo) {
        log.info("根据业务流水号查询转账订单, businessNo: {}", businessNo);
        TransferOrderVO vo = transferService.getTransferOrderByBusinessNo(businessNo);
        return Result.success(vo);
    }

    @PostMapping("/query")
    @ApiOperation("分页查询转账订单列表")
    @Timed(value = "transfer.query.duration", description = "查询转账订单列表耗时")
    public Result<Page<TransferOrderVO>> queryTransferOrders(@RequestBody TransferQueryDTO dto) {
        log.info("查询转账订单列表, fromAccountId: {}, toAccountId: {}, status: {}",
                dto.getFromAccountId(), dto.getToAccountId(), dto.getStatus());
        Page<TransferOrderVO> page = transferService.queryTransferOrders(dto);
        return Result.success(page);
    }
}
