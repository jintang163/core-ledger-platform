package com.bank.core.account.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.service.PaymentService;
import com.bank.core.api.dto.PaymentCallbackDTO;
import com.bank.core.api.dto.PaymentQueryDTO;
import com.bank.core.api.dto.RechargeDTO;
import com.bank.core.api.dto.RefundDTO;
import com.bank.core.api.dto.WithdrawDTO;
import com.bank.core.api.vo.PaymentOrderVO;
import com.bank.core.common.result.Result;
import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 支付核心Controller - 充值与提现
 *
 * 核心功能：
 * 1. 充值：账户入金，增加账户余额
 * 2. 提现：账户出金，减少账户余额
 * 3. 渠道回调：处理第三方支付渠道的异步通知
 * 4. 订单查询：根据订单ID、业务流水号查询支付订单
 * 5. 订单列表：多维度分页查询支付订单
 *
 * 设计要点：
 * - 充值/提现采用"先下单后入账"的异步模式
 * - 渠道回调需要校验签名，防止伪造请求
 * - 支持幂等性，同一请求不会重复处理
 * - 提现需要进行余额校验和风控检查
 * - 热点账户自动使用分片或缓冲记账提高并发性能
 *
 * 业务流程：
 * 充值：用户发起充值 -> 创建支付订单 -> 跳转第三方渠道支付 -> 渠道回调通知 -> 更新账户余额
 * 提现：用户发起提现 -> 创建支付订单 -> 风控校验 -> 调用第三方渠道放款 -> 渠道回调通知 -> 更新账户余额
 *
 * 接口路径：/api/payment/*
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@Api(tags = "支付核心-充值与提现")
@RequiredArgsConstructor
public class PaymentController {

    /** 支付服务接口 */
    private final PaymentService paymentService;

    /**
     * 单账户充值（入金）
     *
     * 业务流程：
     * 1. 校验参数合法性和幂等性
     * 2. 校验账户状态是否正常
     * 3. 创建支付订单，状态为待支付
     * 4. 调用第三方支付渠道创建支付链接
     * 5. 返回支付订单信息和支付链接
     *
     * 注意事项：
     * - 充值订单创建成功不代表资金已到账，需等待渠道回调
     * - 渠道回调成功后才会真正增加账户余额
     * - 同一业务流水号只会创建一个订单
     *
     * @param dto 充值请求参数
     * @return 支付订单信息（包含支付链接）
     */
    @PostMapping("/recharge")
    @ApiOperation("单账户充值（入金）")
    @Timed(value = "payment.recharge.duration", description = "充值请求耗时")
    public Result<PaymentOrderVO> recharge(@RequestBody @Valid RechargeDTO dto) {
        log.info("接收到充值请求, businessNo: {}, accountId: {}, amount: {}",
                dto.getBusinessNo(), dto.getAccountId(), dto.getAmount());
        PaymentOrderVO vo = paymentService.recharge(dto);
        return Result.success(vo);
    }

    /**
     * 单账户提现（出金）
     *
     * 业务流程：
     * 1. 校验参数合法性和幂等性
     * 2. 校验账户状态是否正常
     * 3. 校验账户余额是否充足
     * 4. 进行风控检查（单日限额、单笔限额等）
     * 5. 冻结提现金额（防止重复提现）
     * 6. 创建支付订单，状态为处理中
     * 7. 调用第三方支付渠道进行放款
     * 8. 返回支付订单信息
     *
     * 注意事项：
     * - 提现金额会先冻结，等待渠道回调成功后扣除
     * - 如果渠道放款失败，会自动解冻金额
     * - 同一业务流水号只会创建一个订单
     *
     * @param dto 提现请求参数
     * @return 支付订单信息
     */
    @PostMapping("/withdraw")
    @ApiOperation("单账户提现（出金）")
    @Timed(value = "payment.withdraw.duration", description = "提现请求耗时")
    public Result<PaymentOrderVO> withdraw(@RequestBody @Valid WithdrawDTO dto) {
        log.info("接收到提现请求, businessNo: {}, accountId: {}, amount: {}",
                dto.getBusinessNo(), dto.getAccountId(), dto.getAmount());
        PaymentOrderVO vo = paymentService.withdraw(dto);
        return Result.success(vo);
    }

    /**
     * 渠道回调通知
     *
     * 业务流程：
     * 1. 校验回调签名，防止伪造请求
     * 2. 幂等性校验（同一回调不会重复处理）
     * 3. 根据支付结果更新订单状态
     * 4. 如果支付成功：增加账户余额（充值）或扣除冻结金额（提现）
     * 5. 如果支付失败：解冻金额（提现）或标记订单失败
     * 6. 记录回调日志
     * 7. 返回成功响应给渠道
     *
     * 注意事项：
     * - 必须返回成功响应（200）给渠道，否则渠道会重复回调
     * - 同一订单可能收到多次回调，必须保证幂等
     * - 回调处理失败需要有补偿机制（定时任务轮询）
     *
     * @param dto 渠道回调参数
     * @return 支付订单信息
     */
    @PostMapping("/callback")
    @ApiOperation("渠道回调通知")
    @Timed(value = "payment.callback.duration", description = "渠道回调处理耗时")
    public Result<PaymentOrderVO> handleCallback(@RequestBody @Valid PaymentCallbackDTO dto) {
        log.info("接收到渠道回调, paymentId: {}, channelStatus: {}", dto.getPaymentId(), dto.getChannelStatus());
        PaymentOrderVO vo = paymentService.handleCallback(dto);
        return Result.success(vo);
    }

    /**
     * 根据支付订单ID查询订单详情
     *
     * @param paymentId 支付订单ID
     * @return 支付订单详情
     */
    @GetMapping("/{paymentId}")
    @ApiOperation("查询支付订单详情")
    public Result<PaymentOrderVO> getPaymentOrder(@PathVariable String paymentId) {
        log.info("查询支付订单详情, paymentId: {}", paymentId);
        PaymentOrderVO vo = paymentService.getPaymentOrder(paymentId);
        return Result.success(vo);
    }

    /**
     * 根据业务流水号查询支付订单
     *
     * @param businessNo 业务流水号（调用方传入）
     * @return 支付订单详情
     */
    @GetMapping("/business/{businessNo}")
    @ApiOperation("根据业务流水号查询支付订单")
    public Result<PaymentOrderVO> getPaymentOrderByBusinessNo(@PathVariable String businessNo) {
        log.info("根据业务流水号查询支付订单, businessNo: {}", businessNo);
        PaymentOrderVO vo = paymentService.getPaymentOrderByBusinessNo(businessNo);
        return Result.success(vo);
    }

    /**
     * 分页查询支付订单列表
     *
     * 支持的查询维度：
     * - 账户ID：查询该账户的所有支付订单
     * - 支付类型：按充值/提现过滤
     * - 订单状态：按订单状态过滤
     * - 时间范围：按创建时间范围过滤
     *
     * @param dto 查询条件（包含分页参数）
     * @return 分页的支付订单列表
     */
    @PostMapping("/query")
    @ApiOperation("分页查询支付订单列表")
    @Timed(value = "payment.query.duration", description = "查询支付订单列表耗时")
    public Result<Page<PaymentOrderVO>> queryPaymentOrders(@RequestBody PaymentQueryDTO dto) {
        log.info("查询支付订单列表, accountId: {}, paymentType: {}, status: {}",
                dto.getAccountId(), dto.getPaymentType(), dto.getStatus());
        Page<PaymentOrderVO> page = paymentService.queryPaymentOrders(dto);
        return Result.success(page);
    }

    /**
     * 原路退款 - Saga模式
     * 
     * 业务流程：
     * 1. 创建退款订单（状态：处理中）
     * 2. 调用渠道接口执行退款
     * 3. 清算记账
     * 4. 用户账户入账
     * 
     * 失败时自动逆向补偿，支持事务日志重试
     *
     * @param dto 退款请求参数
     * @return 退款订单信息
     */
    @PostMapping("/refund")
    @ApiOperation("原路退款（Saga模式）")
    @Timed(value = "payment.refund.duration", description = "退款请求耗时")
    public Result<PaymentOrderVO> refund(@RequestBody @Valid RefundDTO dto) {
        log.info("接收到退款请求, originalPaymentId: {}, refundAccountId: {}, amount: {}, businessNo: {}",
                dto.getOriginalPaymentId(), dto.getRefundAccountId(), dto.getAmount(), dto.getBusinessNo());

        PaymentOrderVO vo = paymentService.refund(
                dto.getOriginalPaymentId(),
                dto.getRefundAccountId(),
                dto.getAmount(),
                dto.getCurrency(),
                dto.getBusinessNo(),
                dto.getOperator(),
                dto.getRemark()
        );
        return Result.success(vo);
    }
}
