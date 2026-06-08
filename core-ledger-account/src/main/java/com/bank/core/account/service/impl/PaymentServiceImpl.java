package com.bank.core.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.channel.ChannelAdapter;
import com.bank.core.account.channel.ChannelAdapterFactory;
import com.bank.core.account.channel.ChannelNotificationRequest;
import com.bank.core.account.channel.ChannelNotificationResponse;
import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.PaymentOrder;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.mapper.PaymentOrderMapper;
import com.bank.core.account.service.PaymentService;
import com.bank.core.api.dto.*;
import com.bank.core.api.event.AccountEvent;
import com.bank.core.api.vo.PaymentOrderVO;
import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.*;
import com.bank.core.common.exception.BusinessException;
import com.bank.core.common.utils.AmountUtil;
import com.bank.core.common.utils.DistributedLockUtil;
import com.bank.core.common.utils.IdempotentUtil;
import com.bank.core.common.utils.RateLimitUtil;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 支付服务实现类
 * 处理单账户充值、单账户提现及渠道回调等核心支付业务
 *
 * 核心业务逻辑：
 * 1. 充值：外部渠道 → 内部用户账户（借记银行存款，贷记清算账户）
 * 2. 提现：内部用户账户 → 外部渠道（借记清算账户，贷记银行存款）
 * 3. 回调：处理外部渠道的异步通知，更新订单状态
 *
 * 技术要点：
 * - 全链路幂等性校验（Redis + DB + 分布式锁）
 * - 分布式锁防止并发操作
 * - 异步通知外部渠道
 * - 失败自动冲正机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final AccountMapper accountMapper;
    private final TransactionServiceImpl transactionService;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;
    private final ChannelAdapterFactory channelAdapterFactory;
    private final com.bank.core.account.service.DistributedTransactionService distributedTransactionService;
    private final com.bank.core.account.service.MessageProducerService messageProducerService;

    /**
     * 单账户充值（入金）
     * 资金流向：外部渠道 → 内部用户账户
     *
     * 业务流程：
     * 1. 幂等性校验（Redis + DB + 分布式锁三层校验）
     * 2. 参数校验（币种、金额、账户状态）
     * 3. 分布式锁保证并发安全
     * 4. 创建支付订单（状态：待处理）
     * 5. 创建会计交易（借记银行存款，贷记清算账户）
     * 6. 更新订单状态为成功
     * 7. 发送支付事件到MQ
     * 8. 异步通知外部渠道
     * 9. 清除账户缓存
     *
     * @param dto 充值请求DTO
     * @return 支付订单VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderVO recharge(RechargeDTO dto) {
        log.info("开始充值, businessNo: {}, accountId: {}, amount: {}, channel: {}",
                dto.getBusinessNo(), dto.getAccountId(), dto.getAmount(), dto.getChannelCode());

        RateLimitUtil.checkRateLimit(dto.getAccountId(), CommonConstants.RATE_LIMIT_RECHARGE_SUFFIX);

        try {
            IdempotentUtil.checkIdempotent(CommonConstants.PAYMENT_IDEMPOTENT_PREFIX,
                    dto.getBusinessNo(),
                    CommonConstants.IDEMPOTENT_DEFAULT_TTL_HOURS,
                    TimeUnit.HOURS);

            validateRechargeParam(dto);

            String cachedPaymentId = IdempotentUtil.getIdempotentValue(
                    CommonConstants.PAYMENT_IDEMPOTENT_PREFIX, dto.getBusinessNo());
            if (cachedPaymentId != null && !"PROCESSING".equals(cachedPaymentId)) {
                log.warn("幂等命中（Redis缓存）, businessNo: {}, paymentId: {}", dto.getBusinessNo(), cachedPaymentId);
                PaymentOrder existOrder = paymentOrderMapper.selectByPaymentId(cachedPaymentId);
                if (existOrder != null) {
                    return convertToVO(existOrder);
                }
            }

            PaymentOrder existOrder = paymentOrderMapper.selectByBusinessNo(dto.getBusinessNo());
            if (existOrder != null) {
                log.warn("幂等命中（数据库）, businessNo: {}", dto.getBusinessNo());
                IdempotentUtil.setIdempotentResult(
                        CommonConstants.PAYMENT_IDEMPOTENT_PREFIX,
                        dto.getBusinessNo(),
                        existOrder.getPaymentId(),
                        CommonConstants.IDEMPOTENT_DEFAULT_TTL_HOURS,
                        TimeUnit.HOURS);
                return convertToVO(existOrder);
            }

            String lockKey = CommonConstants.PAYMENT_LOCK_PREFIX + dto.getBusinessNo();
            return DistributedLockUtil.executeWithLock(lockKey, () -> {
                PaymentOrder existAgain = paymentOrderMapper.selectByBusinessNo(dto.getBusinessNo());
                if (existAgain != null) {
                    log.warn("分布式锁内二次检查发现业务单号已存在, businessNo: {}", dto.getBusinessNo());
                    IdempotentUtil.setIdempotentResult(
                            CommonConstants.PAYMENT_IDEMPOTENT_PREFIX,
                            dto.getBusinessNo(),
                            existAgain.getPaymentId(),
                            CommonConstants.IDEMPOTENT_DEFAULT_TTL_HOURS,
                            TimeUnit.HOURS);
                    return convertToVO(existAgain);
                }

                Account account = validateAccount(dto.getAccountId(), dto.getCurrency());

                Account clearingAccount = getClearingAccount(dto.getChannelCode(), dto.getCurrency());

                String paymentId = SnowflakeIdGenerator.nextIdStr();
                String paymentNo = SnowflakeIdGenerator.generatePaymentNo();

                PaymentOrder order = buildPaymentOrder(dto, paymentId, paymentNo, PaymentTypeEnum.RECHARGE);
                paymentOrderMapper.insert(order);

                try {
                    createDepositTransaction(dto, account, clearingAccount, paymentId);

                    order.setStatus(PaymentStatusEnum.SUCCESS.getCode());
                    order.setChannelStatus(ChannelStatusEnum.PENDING.getCode());
                    order.setSuccessTime(LocalDateTime.now());
                    order.setUpdateTime(LocalDateTime.now());
                    paymentOrderMapper.updateById(order);

                    PaymentOrderVO vo = convertToVO(order);

                    IdempotentUtil.setIdempotentResult(
                            CommonConstants.PAYMENT_IDEMPOTENT_PREFIX,
                            dto.getBusinessNo(),
                            paymentId,
                            CommonConstants.IDEMPOTENT_DEFAULT_TTL_HOURS,
                            TimeUnit.HOURS);

                    sendPaymentEvent(vo, CommonConstants.ROCKETMQ_TAG_PAYMENT_RECHARGE);

                    sendChannelNotification(order, ChannelStatusEnum.PENDING);

                    deleteAccountCache(dto.getAccountId());
                    deleteAccountCache(clearingAccount.getAccountId());

                    log.info("充值成功, paymentId: {}, paymentNo: {}, amount: {}", paymentId, paymentNo, dto.getAmount());
                    return vo;
                } catch (Exception e) {
                    log.error("充值失败, paymentId: {}, businessNo: {}", paymentId, dto.getBusinessNo(), e);
                    order.setStatus(PaymentStatusEnum.FAILED.getCode());
                    order.setChannelStatus(ChannelStatusEnum.CHANNEL_FAILED.getCode());
                    order.setUpdateTime(LocalDateTime.now());
                    paymentOrderMapper.updateById(order);
                    throw e;
                }
            });
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                BusinessException be = (BusinessException) e;
                if (!ResultCodeEnum.DUPLICATE_REQUEST.getCode().equals(be.getCode())) {
                    IdempotentUtil.removeIdempotent(CommonConstants.PAYMENT_IDEMPOTENT_PREFIX, dto.getBusinessNo());
                }
            } else {
                IdempotentUtil.removeIdempotent(CommonConstants.PAYMENT_IDEMPOTENT_PREFIX, dto.getBusinessNo());
            }
            throw e;
        }
    }

    /**
     * 单账户提现（出金）
     * 资金流向：内部用户账户 → 外部渠道
     *
     * 业务流程：
     * 1. 幂等性校验（Redis + DB + 分布式锁三层校验）
     * 2. 参数校验（币种、金额、账户状态、余额充足）
     * 3. 分布式锁保证并发安全
     * 4. 创建支付订单（状态：待处理）
     * 5. 创建会计交易（借记清算账户，贷记银行存款）
     * 6. 更新订单状态为处理中（等待渠道处理）
     * 7. 发送支付事件到MQ
     * 8. 异步通知外部渠道处理
     * 9. 清除账户缓存
     *
     * 注意：提现采用异步处理模式，需要等待渠道回调确认最终状态
     * 渠道处理失败时会自动冲正，将资金退回用户账户
     *
     * @param dto 提现请求DTO
     * @return 支付订单VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderVO withdraw(WithdrawDTO dto) {
        log.info("开始提现, businessNo: {}, accountId: {}, amount: {}, channel: {}",
                dto.getBusinessNo(), dto.getAccountId(), dto.getAmount(), dto.getChannelCode());

        RateLimitUtil.checkRateLimit(dto.getAccountId(), CommonConstants.RATE_LIMIT_WITHDRAW_SUFFIX);

        try {
            IdempotentUtil.checkIdempotent(CommonConstants.PAYMENT_IDEMPOTENT_PREFIX,
                    dto.getBusinessNo(),
                    CommonConstants.IDEMPOTENT_DEFAULT_TTL_HOURS,
                    TimeUnit.HOURS);

            validateWithdrawParam(dto);

            String cachedPaymentId = IdempotentUtil.getIdempotentValue(
                    CommonConstants.PAYMENT_IDEMPOTENT_PREFIX, dto.getBusinessNo());
            if (cachedPaymentId != null && !"PROCESSING".equals(cachedPaymentId)) {
                log.warn("幂等命中（Redis缓存）, businessNo: {}, paymentId: {}", dto.getBusinessNo(), cachedPaymentId);
                PaymentOrder existOrder = paymentOrderMapper.selectByPaymentId(cachedPaymentId);
                if (existOrder != null) {
                    return convertToVO(existOrder);
                }
            }

            PaymentOrder existOrder = paymentOrderMapper.selectByBusinessNo(dto.getBusinessNo());
            if (existOrder != null) {
                log.warn("幂等命中（数据库）, businessNo: {}", dto.getBusinessNo());
                IdempotentUtil.setIdempotentResult(
                        CommonConstants.PAYMENT_IDEMPOTENT_PREFIX,
                        dto.getBusinessNo(),
                        existOrder.getPaymentId(),
                        CommonConstants.IDEMPOTENT_DEFAULT_TTL_HOURS,
                        TimeUnit.HOURS);
                return convertToVO(existOrder);
            }

            String lockKey = CommonConstants.PAYMENT_LOCK_PREFIX + dto.getBusinessNo();
            return DistributedLockUtil.executeWithLock(lockKey, () -> {
                PaymentOrder existAgain = paymentOrderMapper.selectByBusinessNo(dto.getBusinessNo());
                if (existAgain != null) {
                    log.warn("分布式锁内二次检查发现业务单号已存在, businessNo: {}", dto.getBusinessNo());
                    IdempotentUtil.setIdempotentResult(
                            CommonConstants.PAYMENT_IDEMPOTENT_PREFIX,
                            dto.getBusinessNo(),
                            existAgain.getPaymentId(),
                            CommonConstants.IDEMPOTENT_DEFAULT_TTL_HOURS,
                            TimeUnit.HOURS);
                    return convertToVO(existAgain);
                }

                Account account = validateAccount(dto.getAccountId(), dto.getCurrency());
                validateSufficientBalance(account, dto.getAmount());

                Account clearingAccount = getClearingAccount(dto.getChannelCode(), dto.getCurrency());

                String paymentId = SnowflakeIdGenerator.nextIdStr();
                String paymentNo = SnowflakeIdGenerator.generatePaymentNo();

                PaymentOrder order = buildPaymentOrder(dto, paymentId, paymentNo, PaymentTypeEnum.WITHDRAW);
                paymentOrderMapper.insert(order);

                try {
                    createWithdrawTransaction(dto, account, clearingAccount, paymentId);

                    order.setStatus(PaymentStatusEnum.PROCESSING.getCode());
                    order.setChannelStatus(ChannelStatusEnum.PENDING.getCode());
                    order.setUpdateTime(LocalDateTime.now());
                    paymentOrderMapper.updateById(order);

                    PaymentOrderVO vo = convertToVO(order);

                    IdempotentUtil.setIdempotentResult(
                            CommonConstants.PAYMENT_IDEMPOTENT_PREFIX,
                            dto.getBusinessNo(),
                            paymentId,
                            CommonConstants.IDEMPOTENT_DEFAULT_TTL_HOURS,
                            TimeUnit.HOURS);

                    sendPaymentEvent(vo, CommonConstants.ROCKETMQ_TAG_PAYMENT_WITHDRAW);

                    sendChannelNotification(order, ChannelStatusEnum.PENDING);

                    deleteAccountCache(dto.getAccountId());
                    deleteAccountCache(clearingAccount.getAccountId());

                    log.info("提现申请成功, 等待渠道处理, paymentId: {}, paymentNo: {}, amount: {}",
                            paymentId, paymentNo, dto.getAmount());
                    return vo;
                } catch (Exception e) {
                    log.error("提现失败, paymentId: {}, businessNo: {}", paymentId, dto.getBusinessNo(), e);
                    order.setStatus(PaymentStatusEnum.FAILED.getCode());
                    order.setChannelStatus(ChannelStatusEnum.CHANNEL_FAILED.getCode());
                    order.setUpdateTime(LocalDateTime.now());
                    paymentOrderMapper.updateById(order);
                    throw e;
                }
            });
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                BusinessException be = (BusinessException) e;
                if (!ResultCodeEnum.DUPLICATE_REQUEST.getCode().equals(be.getCode())) {
                    IdempotentUtil.removeIdempotent(CommonConstants.PAYMENT_IDEMPOTENT_PREFIX, dto.getBusinessNo());
                }
            } else {
                IdempotentUtil.removeIdempotent(CommonConstants.PAYMENT_IDEMPOTENT_PREFIX, dto.getBusinessNo());
            }
            throw e;
        }
    }

    /**
     * 处理渠道回调通知
     * 根据渠道返回的状态更新支付订单状态
     *
     * 业务流程：
     * 1. 校验支付订单是否存在
     * 2. 检查订单是否已处理（幂等）
     * 3. 分布式锁保证并发安全
     * 4. 根据渠道状态更新订单状态
     * 5. 提现失败时自动冲正，将资金退回用户账户
     * 6. 发送回调事件到MQ
     * 7. 清除账户缓存
     *
     * @param dto 渠道回调DTO
     * @return 支付订单VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderVO handleCallback(PaymentCallbackDTO dto) {
        log.info("处理渠道回调, paymentId: {}, channelStatus: {}", dto.getPaymentId(), dto.getChannelStatus());

        // 校验支付订单是否存在
        PaymentOrder order = paymentOrderMapper.selectByPaymentId(dto.getPaymentId());
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_ORDER_NOT_EXIST);
        }

        // 检查订单是否已处理（幂等）
        if (PaymentStatusEnum.SUCCESS.getCode().equals(order.getStatus())
                || PaymentStatusEnum.FAILED.getCode().equals(order.getStatus())) {
            log.warn("支付订单已处理, paymentId: {}, currentStatus: {}", dto.getPaymentId(), order.getStatus());
            return convertToVO(order);
        }

        // 校验渠道状态是否合法
        ChannelStatusEnum channelStatus = ChannelStatusEnum.getByCode(dto.getChannelStatus());
        if (channelStatus == null) {
            throw new BusinessException(ResultCodeEnum.INVALID_CHANNEL_STATUS);
        }

        // 分布式锁：防止同一订单的并发回调
        String lockKey = CommonConstants.PAYMENT_LOCK_PREFIX + dto.getPaymentId();
        return DistributedLockUtil.executeWithLock(lockKey, () -> {
            // 锁内二次校验
            PaymentOrder freshOrder = paymentOrderMapper.selectByPaymentId(dto.getPaymentId());
            if (PaymentStatusEnum.SUCCESS.getCode().equals(freshOrder.getStatus())
                    || PaymentStatusEnum.FAILED.getCode().equals(freshOrder.getStatus())) {
                return convertToVO(freshOrder);
            }

            // 更新渠道信息
            freshOrder.setChannelStatus(dto.getChannelStatus());
            freshOrder.setChannelOrderNo(dto.getChannelOrderNo());
            freshOrder.setChannelTime(dto.getChannelTime() != null ? dto.getChannelTime() : LocalDateTime.now());
            freshOrder.setUpdateTime(LocalDateTime.now());

            // 根据渠道状态更新订单状态
            if (ChannelStatusEnum.CHANNEL_SUCCESS.equals(channelStatus)
                    || ChannelStatusEnum.CALLBACK_SUCCESS.equals(channelStatus)) {
                freshOrder.setStatus(PaymentStatusEnum.SUCCESS.getCode());
                freshOrder.setSuccessTime(LocalDateTime.now());
                log.info("渠道回调成功, paymentId: {}, 订单状态更新为成功", dto.getPaymentId());
            } else if (ChannelStatusEnum.CHANNEL_FAILED.equals(channelStatus)
                    || ChannelStatusEnum.CALLBACK_FAILED.equals(channelStatus)) {
                freshOrder.setStatus(PaymentStatusEnum.FAILED.getCode());
                log.info("渠道回调失败, paymentId: {}, 订单状态更新为失败", dto.getPaymentId());
                // 提现失败时自动冲正，将资金退回用户账户
                if (PaymentTypeEnum.WITHDRAW.getCode().equals(freshOrder.getPaymentType())) {
                    log.info("提现失败, 开始自动冲正, paymentId: {}", dto.getPaymentId());
                    reverseWithdrawTransaction(freshOrder);
                }
            }

            paymentOrderMapper.updateById(freshOrder);

            PaymentOrderVO vo = convertToVO(freshOrder);

            // 发送渠道回调事件到MQ
            sendChannelCallbackEvent(vo);

            // 清除账户缓存
            deleteAccountCache(freshOrder.getAccountId());

            return vo;
        });
    }

    @Override
    public PaymentOrderVO getPaymentOrder(String paymentId) {
        log.debug("查询支付订单, paymentId: {}", paymentId);

        String cacheKey = CommonConstants.PAYMENT_CACHE_PREFIX + paymentId;
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cacheValue = cacheBucket.get();
        if (cacheValue != null) {
            PaymentOrderVO vo = JSON.parseObject(cacheValue, PaymentOrderVO.class);
            log.debug("从缓存获取支付订单成功, paymentId: {}", paymentId);
            return vo;
        }

        PaymentOrder order = paymentOrderMapper.selectByPaymentId(paymentId);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_ORDER_NOT_EXIST);
        }

        PaymentOrderVO vo = convertToVO(order);
        cacheBucket.set(JSON.toJSONString(vo), 30, TimeUnit.MINUTES);

        return vo;
    }

    @Override
    public PaymentOrderVO getPaymentOrderByBusinessNo(String businessNo) {
        log.debug("根据业务流水号查询支付订单, businessNo: {}", businessNo);

        PaymentOrder order = paymentOrderMapper.selectByBusinessNo(businessNo);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_ORDER_NOT_EXIST);
        }

        return convertToVO(order);
    }

    @Override
    public Page<PaymentOrderVO> queryPaymentOrders(PaymentQueryDTO dto) {
        log.info("查询支付订单列表, accountId: {}, paymentType: {}, status: {}",
                dto.getAccountId(), dto.getPaymentType(), dto.getStatus());

        int pageNum = Math.max(dto.getPageNum(), 1);
        int pageSize = Math.min(dto.getPageSize(), CommonConstants.MAX_PAGE_SIZE);

        Page<PaymentOrder> page = new Page<>(pageNum, pageSize);
        Page<PaymentOrder> resultPage = paymentOrderMapper.selectByCondition(
                page,
                dto.getAccountId(),
                dto.getPaymentType(),
                dto.getStatus(),
                dto.getChannelCode(),
                dto.getStartTime(),
                dto.getEndTime()
        );

        Page<PaymentOrderVO> voPage = new Page<>(pageNum, pageSize, resultPage.getTotal());
        List<PaymentOrderVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 校验充值参数
     * @param dto 充值请求DTO
     */
    private void validateRechargeParam(RechargeDTO dto) {
        // 校验渠道是否支持
        if (!channelAdapterFactory.isChannelSupported(dto.getChannelCode())) {
            throw new BusinessException(ResultCodeEnum.CHANNEL_NOT_SUPPORTED,
                    "不支持的渠道: " + dto.getChannelCode());
        }
        // 校验币种
        if (CurrencyEnum.getByCode(dto.getCurrency()) == null) {
            throw new BusinessException(ResultCodeEnum.CURRENCY_NOT_SUPPORTED);
        }
        // 校验金额
        AmountUtil.validateAmount(dto.getAmount());
    }

    /**
     * 校验提现参数
     * @param dto 提现请求DTO
     */
    private void validateWithdrawParam(WithdrawDTO dto) {
        // 校验渠道是否支持
        if (!channelAdapterFactory.isChannelSupported(dto.getChannelCode())) {
            throw new BusinessException(ResultCodeEnum.CHANNEL_NOT_SUPPORTED,
                    "不支持的渠道: " + dto.getChannelCode());
        }
        // 校验币种
        if (CurrencyEnum.getByCode(dto.getCurrency()) == null) {
            throw new BusinessException(ResultCodeEnum.CURRENCY_NOT_SUPPORTED);
        }
        // 校验金额
        AmountUtil.validateAmount(dto.getAmount());
    }

    /**
     * 校验账户状态
     * 检查账户是否存在、是否关闭、是否冻结、币种是否匹配
     * @param accountId 账户ID
     * @param currency 币种
     * @return 账户实体
     */
    private Account validateAccount(String accountId, String currency) {
        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }
        if (AccountStatusEnum.CLOSED.getCode().equals(account.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_CLOSED);
        }
        if (AccountStatusEnum.FROZEN.getCode().equals(account.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_FROZEN);
        }
        if (!currency.equals(account.getCurrency())) {
            throw new BusinessException(ResultCodeEnum.TRANSFER_CURRENCY_MISMATCH);
        }
        return account;
    }

    /**
     * 校验账户余额是否充足
     * @param account 账户实体
     * @param amount 金额（元）
     */
    private void validateSufficientBalance(Account account, BigDecimal amount) {
        long amountFen = AmountUtil.yuanToFen(amount);
        if (account.getBalance() < amountFen) {
            throw new BusinessException(ResultCodeEnum.INSUFFICIENT_BALANCE,
                    "账户余额不足, accountId: " + account.getAccountId() +
                            ", 余额: " + AmountUtil.fenToYuan(account.getBalance()) +
                            ", 需要: " + amount);
        }
    }

    /**
     * 获取渠道对应的清算账户
     * 清算账户是外部渠道在系统内部的代表账户，用于核算与外部渠道的资金往来
     * @param channelCode 渠道编码
     * @param currency 币种
     * @return 清算账户实体
     */
    private Account getClearingAccount(String channelCode, String currency) {
        // 根据渠道编码获取对应的清算账户ID
        String clearingAccountId = ChannelCodeEnum.getClearingAccountId(channelCode);
        if (clearingAccountId == null) {
            throw new BusinessException(ResultCodeEnum.CHANNEL_NOT_SUPPORTED,
                    "渠道未配置清算账户: " + channelCode);
        }

        // 查询清算账户
        Account clearingAccount = accountMapper.selectByAccountId(clearingAccountId);
        if (clearingAccount == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST,
                    "渠道清算账户不存在, accountId: " + clearingAccountId);
        }

        // 校验清算账户状态和币种
        if (!AccountTypeEnum.CLEARING.getCode().equals(clearingAccount.getAccountType())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR,
                    "账户不是清算账户, accountId: " + clearingAccountId);
        }
        if (!currency.equals(clearingAccount.getCurrency())) {
            throw new BusinessException(ResultCodeEnum.TRANSFER_CURRENCY_MISMATCH,
                    "清算账户币种不匹配, 需要: " + currency +
                            ", 实际: " + clearingAccount.getCurrency());
        }

        return clearingAccount;
    }

    /**
     * 构建支付订单实体
     * @param dto 请求DTO（RechargeDTO或WithdrawDTO）
     * @param paymentId 支付订单ID
     * @param paymentNo 支付单号
     * @param type 支付类型
     * @return 支付订单实体
     */
    private PaymentOrder buildPaymentOrder(Object dto, String paymentId, String paymentNo, PaymentTypeEnum type) {
        PaymentOrder order = new PaymentOrder();
        order.setId(SnowflakeIdGenerator.nextId());
        order.setPaymentId(paymentId);
        order.setPaymentNo(paymentNo);
        order.setPaymentType(type.getCode());
        order.setStatus(PaymentStatusEnum.PENDING.getCode());
        order.setChannelStatus(ChannelStatusEnum.PENDING.getCode());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setDeleted(0);

        if (dto instanceof RechargeDTO) {
            RechargeDTO rechargeDTO = (RechargeDTO) dto;
            order.setBusinessNo(rechargeDTO.getBusinessNo());
            order.setRequestId(rechargeDTO.getRequestId());
            order.setAccountId(rechargeDTO.getAccountId());
            order.setAmount(rechargeDTO.getAmount());
            order.setCurrency(rechargeDTO.getCurrency());
            order.setChannelCode(rechargeDTO.getChannelCode());
            order.setChannelOrderNo(rechargeDTO.getChannelOrderNo());
            order.setCallbackUrl(rechargeDTO.getCallbackUrl());
            order.setRemark(rechargeDTO.getRemark());
            order.setOperator(rechargeDTO.getOperator());

            Account account = accountMapper.selectByAccountId(rechargeDTO.getAccountId());
            if (account != null) {
                order.setAccountNo(account.getAccountNo());
            }
        } else if (dto instanceof WithdrawDTO) {
            WithdrawDTO withdrawDTO = (WithdrawDTO) dto;
            order.setBusinessNo(withdrawDTO.getBusinessNo());
            order.setRequestId(withdrawDTO.getRequestId());
            order.setAccountId(withdrawDTO.getAccountId());
            order.setAmount(withdrawDTO.getAmount());
            order.setCurrency(withdrawDTO.getCurrency());
            order.setChannelCode(withdrawDTO.getChannelCode());
            order.setChannelOrderNo(withdrawDTO.getChannelOrderNo());
            order.setCallbackUrl(withdrawDTO.getCallbackUrl());
            order.setRemark(withdrawDTO.getRemark());
            order.setOperator(withdrawDTO.getOperator());

            Account account = accountMapper.selectByAccountId(withdrawDTO.getAccountId());
            if (account != null) {
                order.setAccountNo(account.getAccountNo());
            }
        }

        return order;
    }

    /**
     * 创建充值会计交易
     * 资金流向：外部渠道 → 内部用户账户
     * 会计分录：
     *   借：银行存款（用户账户）  金额
     *   贷：清算账户（渠道账户）  金额
     *
     * 说明：借记用户账户表示用户余额增加，贷记清算账户表示应付给渠道的款项增加
     * （或者应收渠道的款项减少，取决于清算账户的余额方向）
     *
     * @param dto 充值请求DTO
     * @param userAccount 用户账户
     * @param clearingAccount 清算账户
     * @param paymentId 支付订单ID
     */
    private void createDepositTransaction(RechargeDTO dto, Account userAccount, Account clearingAccount, String paymentId) {
        TransactionCreateDTO txDTO = new TransactionCreateDTO();
        txDTO.setRequestId(dto.getRequestId());
        txDTO.setBusinessNo(dto.getBusinessNo() + "_TX");
        txDTO.setTransactionType(TransactionTypeEnum.DEPOSIT.getCode());
        txDTO.setCurrency(dto.getCurrency());
        txDTO.setTotalAmount(dto.getAmount());
        txDTO.setSummary(dto.getRemark() != null ? dto.getRemark() : "账户充值");
        txDTO.setOperator(dto.getOperator());

        List<TransactionEntryDTO> entries = new ArrayList<>();

        // 借方分录：用户账户 - 银行存款增加
        TransactionEntryDTO debitEntry = new TransactionEntryDTO();
        debitEntry.setAccountId(userAccount.getAccountId());
        debitEntry.setSubjectCode("1001");
        debitEntry.setSubjectName("银行存款");
        debitEntry.setDirection(DebitCreditEnum.DEBIT.getCode());
        debitEntry.setAmount(dto.getAmount());
        debitEntry.setSummary("充值入账-用户账户");
        entries.add(debitEntry);

        // 贷方分录：清算账户 - 渠道往来增加（应付给渠道的款项）
        TransactionEntryDTO creditEntry = new TransactionEntryDTO();
        creditEntry.setAccountId(clearingAccount.getAccountId());
        creditEntry.setSubjectCode("2001");
        creditEntry.setSubjectName("清算账户-渠道往来");
        creditEntry.setDirection(DebitCreditEnum.CREDIT.getCode());
        creditEntry.setAmount(dto.getAmount());
        creditEntry.setSummary("充值入账-渠道清算");
        entries.add(creditEntry);

        txDTO.setEntries(entries);

        transactionService.createTransaction(txDTO);
        log.info("充值记账完成, paymentId: {}, 用户账户: {}, 清算账户: {}, 金额: {}",
                paymentId, userAccount.getAccountId(), clearingAccount.getAccountId(), dto.getAmount());
    }

    /**
     * 创建提现会计交易
     * 资金流向：内部用户账户 → 外部渠道
     * 会计分录：
     *   借：清算账户（渠道账户）  金额
     *   贷：银行存款（用户账户）  金额
     *
     * 说明：借记清算账户表示应收渠道的款项增加，贷记用户账户表示用户余额减少
     *
     * @param dto 提现请求DTO
     * @param userAccount 用户账户
     * @param clearingAccount 清算账户
     * @param paymentId 支付订单ID
     */
    private void createWithdrawTransaction(WithdrawDTO dto, Account userAccount, Account clearingAccount, String paymentId) {
        TransactionCreateDTO txDTO = new TransactionCreateDTO();
        txDTO.setRequestId(dto.getRequestId());
        txDTO.setBusinessNo(dto.getBusinessNo() + "_TX");
        txDTO.setTransactionType(TransactionTypeEnum.WITHDRAW.getCode());
        txDTO.setCurrency(dto.getCurrency());
        txDTO.setTotalAmount(dto.getAmount());
        txDTO.setSummary(dto.getRemark() != null ? dto.getRemark() : "账户提现");
        txDTO.setOperator(dto.getOperator());

        List<TransactionEntryDTO> entries = new ArrayList<>();

        // 借方分录：清算账户 - 渠道往来增加（应收渠道的款项）
        TransactionEntryDTO debitEntry = new TransactionEntryDTO();
        debitEntry.setAccountId(clearingAccount.getAccountId());
        debitEntry.setSubjectCode("2001");
        debitEntry.setSubjectName("清算账户-渠道往来");
        debitEntry.setDirection(DebitCreditEnum.DEBIT.getCode());
        debitEntry.setAmount(dto.getAmount());
        debitEntry.setSummary("提现转出-渠道清算");
        entries.add(debitEntry);

        // 贷方分录：用户账户 - 银行存款减少
        TransactionEntryDTO creditEntry = new TransactionEntryDTO();
        creditEntry.setAccountId(userAccount.getAccountId());
        creditEntry.setSubjectCode("1001");
        creditEntry.setSubjectName("银行存款");
        creditEntry.setDirection(DebitCreditEnum.CREDIT.getCode());
        creditEntry.setAmount(dto.getAmount());
        creditEntry.setSummary("提现转出-用户账户");
        entries.add(creditEntry);

        txDTO.setEntries(entries);

        transactionService.createTransaction(txDTO);
        log.info("提现记账完成, paymentId: {}, 用户账户: {}, 清算账户: {}, 金额: {}",
                paymentId, userAccount.getAccountId(), clearingAccount.getAccountId(), dto.getAmount());
    }

    /**
     * 提现失败冲正交易
     * 当渠道通知提现失败时，需要将资金退回用户账户
     * 会计分录（与提现相反）：
     *   借：银行存款（用户账户）  金额
     *   贷：清算账户（渠道账户）  金额
     *
     * @param order 支付订单
     */
    private void reverseWithdrawTransaction(PaymentOrder order) {
        try {
            TransactionCreateDTO txDTO = new TransactionCreateDTO();
            txDTO.setRequestId(order.getRequestId() + "_REV");
            txDTO.setBusinessNo(order.getBusinessNo() + "_REV");
            txDTO.setTransactionType(TransactionTypeEnum.ADJUST.getCode());
            txDTO.setCurrency(order.getCurrency());
            txDTO.setTotalAmount(order.getAmount());
            txDTO.setSummary("提现失败冲正");
            txDTO.setOperator("system");

            // 获取渠道对应的清算账户
            Account clearingAccount = getClearingAccount(order.getChannelCode(), order.getCurrency());

            List<TransactionEntryDTO> entries = new ArrayList<>();

            // 借方分录：用户账户 - 银行存款加回
            TransactionEntryDTO debitEntry = new TransactionEntryDTO();
            debitEntry.setAccountId(order.getAccountId());
            debitEntry.setSubjectCode("1001");
            debitEntry.setSubjectName("银行存款");
            debitEntry.setDirection(DebitCreditEnum.DEBIT.getCode());
            debitEntry.setAmount(order.getAmount());
            debitEntry.setSummary("提现冲正-加回用户余额");
            entries.add(debitEntry);

            // 贷方分录：清算账户 - 渠道往来冲回
            TransactionEntryDTO creditEntry = new TransactionEntryDTO();
            creditEntry.setAccountId(clearingAccount.getAccountId());
            creditEntry.setSubjectCode("2001");
            creditEntry.setSubjectName("清算账户-渠道往来");
            creditEntry.setDirection(DebitCreditEnum.CREDIT.getCode());
            creditEntry.setAmount(order.getAmount());
            creditEntry.setSummary("提现冲正-冲回渠道往来");
            entries.add(creditEntry);

            txDTO.setEntries(entries);

            transactionService.createTransaction(txDTO);
            log.info("提现冲正完成, paymentId: {}, 用户账户: {}, 清算账户: {}, 金额: {}",
                    order.getPaymentId(), order.getAccountId(), clearingAccount.getAccountId(), order.getAmount());

            // 清除清算账户缓存
            deleteAccountCache(clearingAccount.getAccountId());
        } catch (Exception e) {
            log.error("提现冲正失败, paymentId: {}", order.getPaymentId(), e);
        }
    }

    private PaymentOrderVO convertToVO(PaymentOrder order) {
        PaymentOrderVO vo = new PaymentOrderVO();
        BeanUtils.copyProperties(order, vo);

        PaymentTypeEnum typeEnum = PaymentTypeEnum.getByCode(order.getPaymentType());
        if (typeEnum != null) {
            vo.setPaymentTypeDesc(typeEnum.getDesc());
        }

        PaymentStatusEnum statusEnum = PaymentStatusEnum.getByCode(order.getStatus());
        if (statusEnum != null) {
            vo.setStatusDesc(statusEnum.getDesc());
        }

        ChannelStatusEnum channelStatusEnum = ChannelStatusEnum.getByCode(order.getChannelStatus());
        if (channelStatusEnum != null) {
            vo.setChannelStatusDesc(channelStatusEnum.getDesc());
        }

        return vo;
    }

    private void sendPaymentEvent(PaymentOrderVO vo, String tag) {
        try {
            AccountEvent event = new AccountEvent();
            event.setEventId(SnowflakeIdGenerator.nextIdStr());
            event.setEventType(tag);
            event.setAccountId(vo.getAccountId());
            event.setAccountNo(vo.getAccountNo());
            event.setTransactionId(vo.getPaymentId());
            event.setTransactionType(vo.getPaymentType());
            event.setBalance(vo.getAmount());
            event.setRequestId(vo.getRequestId());
            event.setOperator(vo.getOperator());
            event.setEventTime(LocalDateTime.now());

            String destination = CommonConstants.ROCKETMQ_TOPIC_ACCOUNT + ":" + tag;
            rocketMQTemplate.send(destination, MessageBuilder.withPayload(event).build());

            log.info("发送支付事件成功, paymentId: {}, tag: {}", vo.getPaymentId(), tag);
        } catch (Exception e) {
            log.error("发送支付事件失败, paymentId: {}, tag: {}", vo.getPaymentId(), tag, e);
        }
    }

    /**
     * 通知外部渠道
     * 通过渠道适配器工厂获取对应的渠道实现，发送充值或提现通知
     *
     * @param order 支付订单
     * @param status 通知状态
     */
    private void sendChannelNotification(PaymentOrder order, ChannelStatusEnum status) {
        try {
            log.info("通知外部渠道, paymentId: {}, channel: {}, status: {}",
                    order.getPaymentId(), order.getChannelCode(), status.getDesc());

            // 根据渠道编码获取对应的渠道适配器
            ChannelAdapter adapter = channelAdapterFactory.getAdapter(order.getChannelCode());

            // 构建渠道通知请求
            ChannelNotificationRequest request = ChannelNotificationRequest.builder()
                    .paymentId(order.getPaymentId())
                    .paymentNo(order.getPaymentNo())
                    .businessNo(order.getBusinessNo())
                    .channelCode(order.getChannelCode())
                    .channelOrderNo(order.getChannelOrderNo())
                    .paymentType(order.getPaymentType())
                    .amount(order.getAmount())
                    .currency(order.getCurrency())
                    .accountId(order.getAccountId())
                    .accountNo(order.getAccountNo())
                    .channelStatus(status.getCode().toString())
                    .callbackUrl(order.getCallbackUrl())
                    .remark(order.getRemark())
                    .operator(order.getOperator())
                    .requestTime(LocalDateTime.now())
                    .build();

            // 根据支付类型调用不同的渠道方法
            ChannelNotificationResponse response;
            if (PaymentTypeEnum.RECHARGE.getCode().equals(order.getPaymentType())) {
                // 充值通知
                response = adapter.notifyRecharge(request);
            } else if (PaymentTypeEnum.WITHDRAW.getCode().equals(order.getPaymentType())) {
                // 提现通知
                response = adapter.notifyWithdraw(request);
            } else {
                log.warn("未知的支付类型, 跳过渠道通知, paymentId: {}, paymentType: {}",
                        order.getPaymentId(), order.getPaymentType());
                return;
            }

            // 处理渠道响应
            if (response.isSuccess()) {
                log.info("渠道通知成功, paymentId: {}, channelOrderNo: {}",
                        order.getPaymentId(), response.getChannelOrderNo());
                // 如果渠道返回了订单号，更新到订单中
                if (response.getChannelOrderNo() != null && !response.getChannelOrderNo().isEmpty()) {
                    order.setChannelOrderNo(response.getChannelOrderNo());
                    order.setChannelTime(response.getChannelTime() != null ?
                            response.getChannelTime() : LocalDateTime.now());
                    paymentOrderMapper.updateById(order);
                }
            } else {
                log.error("渠道通知失败, paymentId: {}, responseCode: {}, responseMessage: {}",
                        order.getPaymentId(), response.getResponseCode(), response.getResponseMessage());
            }
        } catch (Exception e) {
            log.error("通知外部渠道失败, paymentId: {}", order.getPaymentId(), e);
        }
    }

    private void sendChannelCallbackEvent(PaymentOrderVO vo) {
        try {
            AccountEvent event = new AccountEvent();
            event.setEventId(SnowflakeIdGenerator.nextIdStr());
            event.setEventType(CommonConstants.ROCKETMQ_TAG_CHANNEL_CALLBACK);
            event.setAccountId(vo.getAccountId());
            event.setAccountNo(vo.getAccountNo());
            event.setTransactionId(vo.getPaymentId());
            event.setTransactionType(vo.getPaymentType());
            event.setBalance(vo.getAmount());
            event.setRequestId(vo.getRequestId());
            event.setOperator(vo.getOperator());
            event.setEventTime(LocalDateTime.now());

            String destination = CommonConstants.ROCKETMQ_TOPIC_ACCOUNT + ":"
                    + CommonConstants.ROCKETMQ_TAG_CHANNEL_CALLBACK;
            rocketMQTemplate.send(destination, MessageBuilder.withPayload(event).build());

            log.info("发送渠道回调事件成功, paymentId: {}", vo.getPaymentId());
        } catch (Exception e) {
            log.error("发送渠道回调事件失败, paymentId: {}", vo.getPaymentId(), e);
        }
    }

    @Override
    public PaymentOrderVO refund(String originalPaymentId, String refundAccountId,
                                  java.math.BigDecimal amount, String currency,
                                  String businessNo, String operator, String remark) {
        log.info("开始原路退款, originalPaymentId={}, refundAccountId={}, amount={}",
                originalPaymentId, refundAccountId, amount);

        PaymentOrder originalOrder = paymentOrderMapper.selectByPaymentId(originalPaymentId);
        if (originalOrder == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_ORDER_NOT_EXIST, "原支付订单不存在");
        }

        if (!PaymentStatusEnum.SUCCESS.getCode().equals(originalOrder.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "原支付订单状态不正确，无法退款");
        }

        if (originalOrder.getAmount().compareTo(amount) < 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "退款金额不能大于原支付金额");
        }

        PaymentOrder existingRefundOrder = paymentOrderMapper.selectByBusinessNo(businessNo);
        if (existingRefundOrder != null) {
            log.warn("退款业务单号已存在, businessNo={}, refundPaymentId={}",
                    businessNo, existingRefundOrder.getPaymentId());
            PaymentOrderVO vo = convertPaymentOrderToVO(existingRefundOrder);
            vo.setRemark("退款已提交，Saga事务ID: " + existingRefundOrder.getRemark());
            return vo;
        }

        String refundPaymentId = "REFUND_" + com.bank.core.common.utils.SnowflakeIdGenerator.nextIdStr();
        log.info("创建退款订单, refundPaymentId={}, originalPaymentId={}", refundPaymentId, originalPaymentId);

        PaymentOrder refundOrder = new PaymentOrder();
        refundOrder.setId(com.bank.core.common.utils.SnowflakeIdGenerator.nextId());
        refundOrder.setPaymentId(refundPaymentId);
        refundOrder.setRequestId(originalOrder.getRequestId());
        refundOrder.setBusinessNo(businessNo);
        refundOrder.setPayerAccountId(originalOrder.getPayeeAccountId());
        refundOrder.setPayeeAccountId(refundAccountId);
        refundOrder.setPaymentType(PaymentTypeEnum.REFUND.getCode());
        refundOrder.setAmount(amount);
        refundOrder.setCurrency(currency);
        refundOrder.setStatus(PaymentStatusEnum.PROCESSING.getCode());
        refundOrder.setChannelCode(originalOrder.getChannelCode());
        refundOrder.setOriginalPaymentId(originalPaymentId);
        refundOrder.setOperator(operator);
        refundOrder.setRemark(remark);
        refundOrder.setCreateTime(java.time.LocalDateTime.now());
        refundOrder.setUpdateTime(java.time.LocalDateTime.now());
        refundOrder.setDeleted(0);
        paymentOrderMapper.insert(refundOrder);

        String sagaId;
        try {
            sagaId = distributedTransactionService.executeRefundWithSaga(
                    originalPaymentId, refundAccountId, amount, currency, businessNo, operator, remark
            );
        } catch (Exception e) {
            log.error("退款Saga执行失败, refundPaymentId={}, originalPaymentId={}",
                    refundPaymentId, originalPaymentId, e);
            refundOrder.setStatus(PaymentStatusEnum.FAILED.getCode());
            refundOrder.setRemark("退款失败: " + e.getMessage());
            refundOrder.setUpdateTime(java.time.LocalDateTime.now());
            paymentOrderMapper.updateById(refundOrder);
            throw e;
        }

        refundOrder.setStatus(PaymentStatusEnum.SUCCESS.getCode());
        refundOrder.setPaymentTime(java.time.LocalDateTime.now());
        refundOrder.setRemark("Saga事务ID: " + sagaId + (remark != null ? ", " + remark : ""));
        refundOrder.setUpdateTime(java.time.LocalDateTime.now());
        paymentOrderMapper.updateById(refundOrder);

        log.info("原路退款处理完成, refundPaymentId={}, sagaId={}, originalPaymentId={}, amount={}",
                refundPaymentId, sagaId, originalPaymentId, amount);

        PaymentOrderVO vo = convertPaymentOrderToVO(refundOrder);
        vo.setSagaId(sagaId);
        vo.setOriginalPaymentId(originalPaymentId);
        vo.setRemark("退款已提交，Saga事务ID: " + sagaId);

        try {
            messageProducerService.sendRefundSuccessMessage(vo);
        } catch (Exception e) {
            log.error("发送退款成功消息失败, refundPaymentId={}", refundPaymentId, e);
        }



        return vo;
    }

    @Override
    public PaymentOrderVO refund(com.bank.core.api.dto.RefundDTO dto) {
        PaymentOrderVO vo = refund(
                dto.getOriginalPaymentId(),
                dto.getRefundAccountId(),
                dto.getAmount(),
                dto.getCurrency(),
                dto.getBusinessNo(),
                dto.getOperator(),
                dto.getRemark()
        );

        try {
            if (dto.getCallbackUrl() != null && !dto.getCallbackUrl().trim().isEmpty()) {
                java.util.Map<String, Object> additionalData = new java.util.HashMap<>();
                additionalData.put("sagaId", vo.getSagaId());
                additionalData.put("originalPaymentId", dto.getOriginalPaymentId());
                messageProducerService.sendCallbackNotify(
                        "REFUND_SUCCESS",
                        dto.getCallbackUrl(),
                        buildCallbackData(vo, additionalData),
                        dto.getBusinessNo()
                );
            }
        } catch (Exception e) {
            log.error("发送退款回调失败, refundPaymentId={}", vo.getPaymentId(), e);
        }

        return vo;
    }

    private void deleteAccountCache(String accountId) {
        IdempotentUtil.deleteAccountCache(accountId);
    }

    private java.util.Map<String, Object> buildCallbackData(PaymentOrderVO vo, java.util.Map<String, Object> additionalData) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("paymentId", vo.getPaymentId());
        data.put("businessNo", vo.getBusinessNo());
        data.put("payerAccountId", vo.getPayerAccountId());
        data.put("payeeAccountId", vo.getPayeeAccountId());
        data.put("amount", vo.getAmount());
        data.put("currency", vo.getCurrency());
        data.put("paymentType", vo.getPaymentType());
        data.put("paymentTypeDesc", vo.getPaymentTypeDesc());
        data.put("status", vo.getStatus());
        data.put("statusDesc", vo.getStatusDesc());
        data.put("paymentTime", vo.getPaymentTime());
        data.put("channelCode", vo.getChannelCode());
        data.put("remark", vo.getRemark());
        data.put("sagaId", vo.getSagaId());
        data.put("originalPaymentId", vo.getOriginalPaymentId());

        if (additionalData != null) {
            data.putAll(additionalData);
        }
        return data;
    }
}
