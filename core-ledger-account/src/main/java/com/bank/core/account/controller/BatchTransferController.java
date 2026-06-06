package com.bank.core.account.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.service.BatchTransferService;
import com.bank.core.api.dto.BatchTransferDTO;
import com.bank.core.api.dto.BatchTransferQueryDTO;
import com.bank.core.api.vo.BatchTransferItemVO;
import com.bank.core.api.vo.BatchTransferOrderVO;
import com.bank.core.common.result.Result;
import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/batch-transfer")
@Api(tags = "支付核心-批量转账（代发）")
@RequiredArgsConstructor
public class BatchTransferController {

    private final BatchTransferService batchTransferService;

    @PostMapping("/create")
    @ApiOperation("批量转账（代发）- 支持部分成功")
    @Timed(value = "batch.transfer.create.duration", description = "批量转账请求耗时")
    public Result<BatchTransferOrderVO> batchTransfer(@RequestBody @Valid BatchTransferDTO dto) {
        log.info("接收到批量转账请求, businessNo: {}, fromAccountId: {}, itemCount: {}",
                dto.getBusinessNo(), dto.getFromAccountId(), dto.getItems().size());
        BatchTransferOrderVO vo = batchTransferService.batchTransfer(dto);
        return Result.success(vo);
    }

    @GetMapping("/{batchId}")
    @ApiOperation("查询批量转账订单详情")
    public Result<BatchTransferOrderVO> getBatchTransferOrder(@PathVariable String batchId) {
        log.info("查询批量转账订单详情, batchId: {}", batchId);
        BatchTransferOrderVO vo = batchTransferService.getBatchTransferOrder(batchId);
        return Result.success(vo);
    }

    @GetMapping("/business/{businessNo}")
    @ApiOperation("根据业务流水号查询批量转账订单")
    public Result<BatchTransferOrderVO> getBatchTransferOrderByBusinessNo(@PathVariable String businessNo) {
        log.info("根据业务流水号查询批量转账订单, businessNo: {}", businessNo);
        BatchTransferOrderVO vo = batchTransferService.getBatchTransferOrderByBusinessNo(businessNo);
        return Result.success(vo);
    }

    @GetMapping("/{batchId}/items")
    @ApiOperation("查询批量转账明细列表")
    public Result<List<BatchTransferItemVO>> getBatchTransferItems(@PathVariable String batchId) {
        log.info("查询批量转账明细列表, batchId: {}", batchId);
        List<BatchTransferItemVO> items = batchTransferService.getBatchTransferItems(batchId);
        return Result.success(items);
    }

    @PostMapping("/query")
    @ApiOperation("分页查询批量转账订单列表")
    @Timed(value = "batch.transfer.query.duration", description = "查询批量转账订单列表耗时")
    public Result<Page<BatchTransferOrderVO>> queryBatchTransferOrders(@RequestBody BatchTransferQueryDTO dto) {
        log.info("查询批量转账订单列表, fromAccountId: {}, status: {}",
                dto.getFromAccountId(), dto.getStatus());
        Page<BatchTransferOrderVO> page = batchTransferService.queryBatchTransferOrders(dto);
        return Result.success(page);
    }
}
