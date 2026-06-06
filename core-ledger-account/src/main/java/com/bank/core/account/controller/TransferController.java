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

/**
 * 支付核心Controller - 账户间转账
 *
 * 核心功能：
 * 1. 账户间转账：同一系统内两个账户之间的资金转移
 * 2. 订单查询：根据订单ID、业务流水号查询转账订单
 * 3. 订单列表：多维度分页查询转账订单
 *
 * 设计要点：
 * - 转账采用同步模式，实时更新双方账户余额
 * - 使用分布式锁防止并发冲突（按账户ID排序避免死锁）
 * - 支持幂等性，同一请求不会重复转账
 * - 转出账户需要进行余额校验
 * - 热点账户自动使用分片或缓冲记账提高并发性能
 * - 转账失败自动回滚，保证数据一致性
 *
 * 业务流程：
 * 1. 校验参数合法性和幂等性
 * 2. 校验转出/转入账户状态是否正常
 * 3. 校验转出账户余额是否充足
 * 4. 获取两个账户的分布式锁（按ID排序避免死锁）
 * 5. 判断是否为热点账户，决定是否使用分片或缓冲
 * 6. 执行复式记账：转出账户扣款，转入账户加款
 * 7. 创建转账订单，状态为成功
 * 8. 返回转账订单信息
 *
 * 接口路径：/api/transfer/*
 */
@Slf4j
@RestController
@RequestMapping("/api/transfer")
@Api(tags = "支付核心-账户间转账")
@RequiredArgsConstructor
public class TransferController {

    /** 转账服务接口 */
    private final TransferService transferService;

    /**
     * 账户间转账
     *
     * 业务流程：
     * 1. 校验参数合法性和幂等性
     * 2. 校验转出/转入账户状态是否正常
     * 3. 校验转出账户余额是否充足
     * 4. 获取两个账户的分布式锁（按ID排序避免死锁）
     * 5. 判断是否为热点账户，决定是否使用分片或缓冲
     * 6. 执行复式记账：转出账户扣款，转入账户加款
     * 7. 创建转账订单，状态为成功
     * 8. 返回转账订单信息
     *
     * 注意事项：
     * - 转账为实时交易，成功或失败立即返回
     * - 同一业务流水号只会执行一次转账
     * - 使用分布式锁保证原子性，防止并发冲突
     * - 转出账户和转入账户不能是同一个账户
     * - 支持跨币种转账（需配置汇率）
     *
     * @param dto 转账请求参数
     * @return 转账订单信息
     */
    @PostMapping("/create")
    @ApiOperation("账户间转账")
    @Timed(value = "transfer.create.duration", description = "转账请求耗时")
    public Result<TransferOrderVO> transfer(@RequestBody @Valid TransferDTO dto) {
        log.info("接收到转账请求, businessNo: {}, fromAccountId: {}, toAccountId: {}, amount: {}",
                dto.getBusinessNo(), dto.getFromAccountId(), dto.getToAccountId(), dto.getAmount());
        TransferOrderVO vo = transferService.transfer(dto);
        return Result.success(vo);
    }

    /**
     * 根据转账订单ID查询订单详情
     *
     * @param transferId 转账订单ID
     * @return 转账订单详情
     */
    @GetMapping("/{transferId}")
    @ApiOperation("查询转账订单详情")
    public Result<TransferOrderVO> getTransferOrder(@PathVariable String transferId) {
        log.info("查询转账订单详情, transferId: {}", transferId);
        TransferOrderVO vo = transferService.getTransferOrder(transferId);
        return Result.success(vo);
    }

    /**
     * 根据业务流水号查询转账订单
     *
     * @param businessNo 业务流水号（调用方传入）
     * @return 转账订单详情
     */
    @GetMapping("/business/{businessNo}")
    @ApiOperation("根据业务流水号查询转账订单")
    public Result<TransferOrderVO> getTransferOrderByBusinessNo(@PathVariable String businessNo) {
        log.info("根据业务流水号查询转账订单, businessNo: {}", businessNo);
        TransferOrderVO vo = transferService.getTransferOrderByBusinessNo(businessNo);
        return Result.success(vo);
    }

    /**
     * 分页查询转账订单列表
     *
     * 支持的查询维度：
     * - 转出账户ID：查询该账户的转出记录
     * - 转入账户ID：查询该账户的转入记录
     * - 订单状态：按订单状态过滤
     * - 时间范围：按创建时间范围过滤
     *
     * @param dto 查询条件（包含分页参数）
     * @return 分页的转账订单列表
     */
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
