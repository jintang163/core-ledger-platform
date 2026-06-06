package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体
 * 存储充值和提现的支付订单信息
 * 支持与外部渠道的交互和状态跟踪
 */
@Data
@TableName("t_payment_order")
public class PaymentOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（雪花算法生成） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 支付订单ID（业务主键） */
    private String paymentId;

    /** 支付单号（业务流水号，对外展示） */
    private String paymentNo;

    /** 业务流水号（调用方传入，用于幂等） */
    private String businessNo;

    /** 请求ID（用于幂等校验） */
    private String requestId;

    /** 支付类型：1-充值 2-提现 */
    private Integer paymentType;

    /** 账户ID */
    private String accountId;

    /** 账户号 */
    private String accountNo;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 币种 */
    private String currency;

    /** 渠道编码（ALIPAY, WECHAT, UNIONPAY等） */
    private String channelCode;

    /** 渠道订单号（外部渠道返回的订单号） */
    private String channelOrderNo;

    /** 订单状态：0-待处理 1-处理中 2-成功 3-失败 */
    private Integer status;

    /** 渠道状态：0-待通知 1-渠道成功 2-渠道失败 3-回调成功 4-回调失败 */
    private Integer channelStatus;

    /** 回调地址（渠道处理完成后回调的地址） */
    private String callbackUrl;

    /** 备注 */
    private String remark;

    /** 操作员 */
    private String operator;

    /** 渠道处理时间 */
    private LocalDateTime channelTime;

    /** 成功时间 */
    private LocalDateTime successTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标识：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
