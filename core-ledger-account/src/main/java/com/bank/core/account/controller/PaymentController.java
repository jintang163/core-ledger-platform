package com.bank.core.account.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.service.PaymentService;
import com.bank.core.api.dto.PaymentCallbackDTO;
import com.bank.core.api.dto.PaymentQueryDTO;
import com.bank.core.api.dto.RechargeDTO;
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

@Slf4j
@RestController
@RequestMapping("/api/payment")
@Api(tags = "支付核心-充值与提现")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/recharge")
    @ApiOperation("单账户充值（入金）")
    @Timed(value = "payment.recharge.duration", description = "充值请求耗时")
    public Result<PaymentOrderVO> recharge(@RequestBody @Valid RechargeDTO dto) {
        log.info("接收到充值请求, businessNo: {}, accountId: {}, amount: {}",
                dto.getBusinessNo(), dto.getAccountId(), dto.getAmount());
        PaymentOrderVO vo = paymentService.recharge(dto);
        return Result.success(vo);
    }

    @PostMapping("/withdraw")
    @ApiOperation("单账户提现（出金）")
    @Timed(value = "payment.withdraw.duration", description = "提现请求耗时")
    public Result<PaymentOrderVO> withdraw(@RequestBody @Valid WithdrawDTO dto) {
        log.info("接收到提现请求, businessNo: {}, accountId: {}, amount: {}",
                dto.getBusinessNo(), dto.getAccountId(), dto.getAmount());
        PaymentOrderVO vo = paymentService.withdraw(dto);
        return Result.success(vo);
    }

    @PostMapping("/callback")
    @ApiOperation("渠道回调通知")
    @Timed(value = "payment.callback.duration", description = "渠道回调处理耗时")
    public Result<PaymentOrderVO> handleCallback(@RequestBody @Valid PaymentCallbackDTO dto) {
        log.info("接收到渠道回调, paymentId: {}, channelStatus: {}", dto.getPaymentId(), dto.getChannelStatus());
        PaymentOrderVO vo = paymentService.handleCallback(dto);
        return Result.success(vo);
    }

    @GetMapping("/{paymentId}")
    @ApiOperation("查询支付订单详情")
    public Result<PaymentOrderVO> getPaymentOrder(@PathVariable String paymentId) {
        log.info("查询支付订单详情, paymentId: {}", paymentId);
        PaymentOrderVO vo = paymentService.getPaymentOrder(paymentId);
        return Result.success(vo);
    }

    @GetMapping("/business/{businessNo}")
    @ApiOperation("根据业务流水号查询支付订单")
    public Result<PaymentOrderVO> getPaymentOrderByBusinessNo(@PathVariable String businessNo) {
        log.info("根据业务流水号查询支付订单, businessNo: {}", businessNo);
        PaymentOrderVO vo = paymentService.getPaymentOrderByBusinessNo(businessNo);
        return Result.success(vo);
    }

    @PostMapping("/query")
    @ApiOperation("分页查询支付订单列表")
    @Timed(value = "payment.query.duration", description = "查询支付订单列表耗时")
    public Result<Page<PaymentOrderVO>> queryPaymentOrders(@RequestBody PaymentQueryDTO dto) {
        log.info("查询支付订单列表, accountId: {}, paymentType: {}, status: {}",
                dto.getAccountId(), dto.getPaymentType(), dto.getStatus());
        Page<PaymentOrderVO> page = paymentService.queryPaymentOrders(dto);
        return Result.success(page);
    }
}
