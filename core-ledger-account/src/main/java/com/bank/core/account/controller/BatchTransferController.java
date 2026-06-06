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

/**
 * 支付核心Controller - 批量转账（代发）
 *
 * 核心功能：
 * 1. 批量转账：从一个账户向多个账户转账（如工资代发、佣金结算）
 * 2. 支持部分成功：单笔失败不影响其他笔
 * 3. 订单查询：根据订单ID、业务流水号查询批量转账订单
 * 4. 明细查询：查询批量订单的每一笔转账明细
 * 5. 订单列表：多维度分页查询批量转账订单
 *
 * 设计要点：
 * - 采用"先冻结总金额，后逐笔转账"的模式
 * - 支持部分成功，单笔失败自动回滚该笔
 * - 热点账户自动使用缓冲记账提高并发性能
 * - 支持幂等性，同一请求不会重复处理
 * - 提供进度查询，便于调用方追踪处理状态
 * - 转账明细支持导出，便于对账
 *
 * 业务流程：
 * 1. 校验参数合法性和幂等性
 * 2. 校验转出账户状态和余额是否充足（总金额）
 * 3. 冻结转出账户的总转账金额
 * 4. 创建批量转账订单，状态为处理中
 * 5. 异步逐笔处理每一条转账明细
 * 6. 每笔处理完成后更新明细状态
 * 7. 全部处理完成后更新订单状态为成功/部分成功/失败
 * 8. 解冻剩余未转账金额（如有）
 *
 * 适用场景：
 * - 工资代发：企业向员工发工资
 * - 佣金结算：平台向商户/达人结算佣金
 * - 补贴发放：政府/企业向个人发放补贴
 * - 退款批量处理：电商平台批量退款
 *
 * 接口路径：/api/batch-transfer/*
 */
@Slf4j
@RestController
@RequestMapping("/api/batch-transfer")
@Api(tags = "支付核心-批量转账（代发）")
@RequiredArgsConstructor
public class BatchTransferController {

    /** 批量转账服务接口 */
    private final BatchTransferService batchTransferService;

    /**
     * 批量转账（代发）- 支持部分成功
     *
     * 业务流程：
     * 1. 校验参数合法性和幂等性
     * 2. 校验转出账户状态是否正常
     * 3. 校验总金额是否充足，单笔金额是否符合限额
     * 4. 校验收款方账户是否都存在
     * 5. 冻结转出账户的总转账金额
     * 6. 创建批量转账订单和明细，状态为处理中
     * 7. 异步逐笔处理每一条转账明细
     * 8. 返回批量订单信息（含批次ID）
     *
     * 注意事项：
     * - 接口立即返回，转账处理在后台异步执行
     * - 单笔转账失败不影响其他笔，支持部分成功
     * - 同一业务流水号只会创建一个批次
     * - 总金额 = 所有明细金额之和 + 手续费（如有）
     * - 如果转出账户是热点账户，自动使用缓冲记账
     * - 调用方需要通过查询接口获取最终处理结果
     *
     * @param dto 批量转账请求参数
     * @return 批量转账订单信息（包含批次ID）
     */
    @PostMapping("/create")
    @ApiOperation("批量转账（代发）- 支持部分成功")
    @Timed(value = "batch.transfer.create.duration", description = "批量转账请求耗时")
    public Result<BatchTransferOrderVO> batchTransfer(@RequestBody @Valid BatchTransferDTO dto) {
        log.info("接收到批量转账请求, businessNo: {}, fromAccountId: {}, itemCount: {}",
                dto.getBusinessNo(), dto.getFromAccountId(), dto.getItems().size());
        BatchTransferOrderVO vo = batchTransferService.batchTransfer(dto);
        return Result.success(vo);
    }

    /**
     * 根据批量转账订单ID查询订单详情
     *
     * @param batchId 批量转账订单ID（批次ID）
     * @return 批量转账订单详情（包含成功/失败数量统计）
     */
    @GetMapping("/{batchId}")
    @ApiOperation("查询批量转账订单详情")
    public Result<BatchTransferOrderVO> getBatchTransferOrder(@PathVariable String batchId) {
        log.info("查询批量转账订单详情, batchId: {}", batchId);
        BatchTransferOrderVO vo = batchTransferService.getBatchTransferOrder(batchId);
        return Result.success(vo);
    }

    /**
     * 根据业务流水号查询批量转账订单
     *
     * @param businessNo 业务流水号（调用方传入）
     * @return 批量转账订单详情
     */
    @GetMapping("/business/{businessNo}")
    @ApiOperation("根据业务流水号查询批量转账订单")
    public Result<BatchTransferOrderVO> getBatchTransferOrderByBusinessNo(@PathVariable String businessNo) {
        log.info("根据业务流水号查询批量转账订单, businessNo: {}", businessNo);
        BatchTransferOrderVO vo = batchTransferService.getBatchTransferOrderByBusinessNo(businessNo);
        return Result.success(vo);
    }

    /**
     * 查询批量转账明细列表
     *
     * @param batchId 批量转账订单ID（批次ID）
     * @return 该批次的所有转账明细列表（包含每笔的成功/失败状态）
     */
    @GetMapping("/{batchId}/items")
    @ApiOperation("查询批量转账明细列表")
    public Result<List<BatchTransferItemVO>> getBatchTransferItems(@PathVariable String batchId) {
        log.info("查询批量转账明细列表, batchId: {}", batchId);
        List<BatchTransferItemVO> items = batchTransferService.getBatchTransferItems(batchId);
        return Result.success(items);
    }

    /**
     * 分页查询批量转账订单列表
     *
     * 支持的查询维度：
     * - 转出账户ID：查询该账户的批量转出记录
     * - 订单状态：按订单状态过滤（待处理/处理中/成功/部分成功/失败）
     * - 时间范围：按创建时间范围过滤
     *
     * @param dto 查询条件（包含分页参数）
     * @return 分页的批量转账订单列表
     */
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
