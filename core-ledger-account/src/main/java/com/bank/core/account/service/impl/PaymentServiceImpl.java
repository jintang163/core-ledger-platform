package com.bank.core.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final AccountMapper accountMapper;
    private final TransactionServiceImpl transactionService;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderVO recharge(RechargeDTO dto) {
        log.info("开始充值, businessNo: {}, accountId: {}, amount: {}",
                dto.getBusinessNo(), dto.getAccountId(), dto.getAmount());

        IdempotentUtil.checkIdempotent(dto.getRequestId());

        validateRechargeParam(dto);

        String idempotentKey = CommonConstants.PAYMENT_IDEMPOTENT_PREFIX + dto.getBusinessNo();
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);
        String cachedPaymentId = idempotentBucket.get();
        if (cachedPaymentId != null) {
            log.warn("幂等命中, businessNo: {}, paymentId: {}", dto.getBusinessNo(), cachedPaymentId);
            PaymentOrder existOrder = paymentOrderMapper.selectByPaymentId(cachedPaymentId);
            if (existOrder != null) {
                return convertToVO(existOrder);
            }
        }

        PaymentOrder existOrder = paymentOrderMapper.selectByBusinessNo(dto.getBusinessNo());
        if (existOrder != null) {
            log.warn("业务单号已存在, businessNo: {}", dto.getBusinessNo());
            idempotentBucket.set(existOrder.getPaymentId(), 24, TimeUnit.HOURS);
            return convertToVO(existOrder);
        }

        String lockKey = CommonConstants.PAYMENT_LOCK_PREFIX + dto.getBusinessNo();
        return DistributedLockUtil.executeWithLock(lockKey, () -> {
            PaymentOrder existAgain = paymentOrderMapper.selectByBusinessNo(dto.getBusinessNo());
            if (existAgain != null) {
                log.warn("分布式锁内二次检查发现业务单号已存在, businessNo: {}", dto.getBusinessNo());
                idempotentBucket.set(existAgain.getPaymentId(), 24, TimeUnit.HOURS);
                return convertToVO(existAgain);
            }

            Account account = validateAccount(dto.getAccountId(), dto.getCurrency());

            String paymentId = SnowflakeIdGenerator.nextIdStr();
            String paymentNo = SnowflakeIdGenerator.generatePaymentNo();

            PaymentOrder order = buildPaymentOrder(dto, paymentId, paymentNo, PaymentTypeEnum.RECHARGE);
            paymentOrderMapper.insert(order);

            try {
                createDepositTransaction(dto, account, paymentId);

                order.setStatus(PaymentStatusEnum.SUCCESS.getCode());
                order.setChannelStatus(ChannelStatusEnum.PENDING.getCode());
                order.setSuccessTime(LocalDateTime.now());
                order.setUpdateTime(LocalDateTime.now());
                paymentOrderMapper.updateById(order);

                PaymentOrderVO vo = convertToVO(order);

                idempotentBucket.set(paymentId, 24, TimeUnit.HOURS);

                sendPaymentEvent(vo, CommonConstants.ROCKETMQ_TAG_PAYMENT_RECHARGE);

                sendChannelNotification(order, ChannelStatusEnum.PENDING);

                deleteAccountCache(dto.getAccountId());

                log.info("充值成功, paymentId: {}, paymentNo: {}", paymentId, paymentNo);
                return vo;
            } catch (Exception e) {
                log.error("充值失败, paymentId: {}", paymentId, e);
                order.setStatus(PaymentStatusEnum.FAILED.getCode());
                order.setChannelStatus(ChannelStatusEnum.CHANNEL_FAILED.getCode());
                order.setUpdateTime(LocalDateTime.now());
                paymentOrderMapper.updateById(order);
                throw e;
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderVO withdraw(WithdrawDTO dto) {
        log.info("开始提现, businessNo: {}, accountId: {}, amount: {}",
                dto.getBusinessNo(), dto.getAccountId(), dto.getAmount());

        IdempotentUtil.checkIdempotent(dto.getRequestId());

        validateWithdrawParam(dto);

        String idempotentKey = CommonConstants.PAYMENT_IDEMPOTENT_PREFIX + dto.getBusinessNo();
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);
        String cachedPaymentId = idempotentBucket.get();
        if (cachedPaymentId != null) {
            log.warn("幂等命中, businessNo: {}, paymentId: {}", dto.getBusinessNo(), cachedPaymentId);
            PaymentOrder existOrder = paymentOrderMapper.selectByPaymentId(cachedPaymentId);
            if (existOrder != null) {
                return convertToVO(existOrder);
            }
        }

        PaymentOrder existOrder = paymentOrderMapper.selectByBusinessNo(dto.getBusinessNo());
        if (existOrder != null) {
            log.warn("业务单号已存在, businessNo: {}", dto.getBusinessNo());
            idempotentBucket.set(existOrder.getPaymentId(), 24, TimeUnit.HOURS);
            return convertToVO(existOrder);
        }

        String lockKey = CommonConstants.PAYMENT_LOCK_PREFIX + dto.getBusinessNo();
        return DistributedLockUtil.executeWithLock(lockKey, () -> {
            PaymentOrder existAgain = paymentOrderMapper.selectByBusinessNo(dto.getBusinessNo());
            if (existAgain != null) {
                log.warn("分布式锁内二次检查发现业务单号已存在, businessNo: {}", dto.getBusinessNo());
                idempotentBucket.set(existAgain.getPaymentId(), 24, TimeUnit.HOURS);
                return convertToVO(existAgain);
            }

            Account account = validateAccount(dto.getAccountId(), dto.getCurrency());
            validateSufficientBalance(account, dto.getAmount());

            String paymentId = SnowflakeIdGenerator.nextIdStr();
            String paymentNo = SnowflakeIdGenerator.generatePaymentNo();

            PaymentOrder order = buildPaymentOrder(dto, paymentId, paymentNo, PaymentTypeEnum.WITHDRAW);
            paymentOrderMapper.insert(order);

            try {
                createWithdrawTransaction(dto, account, paymentId);

                order.setStatus(PaymentStatusEnum.PROCESSING.getCode());
                order.setChannelStatus(ChannelStatusEnum.PENDING.getCode());
                order.setUpdateTime(LocalDateTime.now());
                paymentOrderMapper.updateById(order);

                PaymentOrderVO vo = convertToVO(order);

                idempotentBucket.set(paymentId, 24, TimeUnit.HOURS);

                sendPaymentEvent(vo, CommonConstants.ROCKETMQ_TAG_PAYMENT_WITHDRAW);

                sendChannelNotification(order, ChannelStatusEnum.PENDING);

                deleteAccountCache(dto.getAccountId());

                log.info("提现申请成功, 等待渠道处理, paymentId: {}, paymentNo: {}", paymentId, paymentNo);
                return vo;
            } catch (Exception e) {
                log.error("提现失败, paymentId: {}", paymentId, e);
                order.setStatus(PaymentStatusEnum.FAILED.getCode());
                order.setChannelStatus(ChannelStatusEnum.CHANNEL_FAILED.getCode());
                order.setUpdateTime(LocalDateTime.now());
                paymentOrderMapper.updateById(order);
                throw e;
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderVO handleCallback(PaymentCallbackDTO dto) {
        log.info("处理渠道回调, paymentId: {}, channelStatus: {}", dto.getPaymentId(), dto.getChannelStatus());

        PaymentOrder order = paymentOrderMapper.selectByPaymentId(dto.getPaymentId());
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_ORDER_NOT_EXIST);
        }

        if (PaymentStatusEnum.SUCCESS.getCode().equals(order.getStatus())
                || PaymentStatusEnum.FAILED.getCode().equals(order.getStatus())) {
            log.warn("支付订单已处理, paymentId: {}, currentStatus: {}", dto.getPaymentId(), order.getStatus());
            return convertToVO(order);
        }

        ChannelStatusEnum channelStatus = ChannelStatusEnum.getByCode(dto.getChannelStatus());
        if (channelStatus == null) {
            throw new BusinessException(ResultCodeEnum.INVALID_CHANNEL_STATUS);
        }

        String lockKey = CommonConstants.PAYMENT_LOCK_PREFIX + dto.getPaymentId();
        return DistributedLockUtil.executeWithLock(lockKey, () -> {
            PaymentOrder freshOrder = paymentOrderMapper.selectByPaymentId(dto.getPaymentId());
            if (PaymentStatusEnum.SUCCESS.getCode().equals(freshOrder.getStatus())
                    || PaymentStatusEnum.FAILED.getCode().equals(freshOrder.getStatus())) {
                return convertToVO(freshOrder);
            }

            freshOrder.setChannelStatus(dto.getChannelStatus());
            freshOrder.setChannelOrderNo(dto.getChannelOrderNo());
            freshOrder.setChannelTime(dto.getChannelTime() != null ? dto.getChannelTime() : LocalDateTime.now());
            freshOrder.setUpdateTime(LocalDateTime.now());

            if (ChannelStatusEnum.CHANNEL_SUCCESS.equals(channelStatus)
                    || ChannelStatusEnum.CALLBACK_SUCCESS.equals(channelStatus)) {
                freshOrder.setStatus(PaymentStatusEnum.SUCCESS.getCode());
                freshOrder.setSuccessTime(LocalDateTime.now());
                log.info("渠道回调成功, paymentId: {}", dto.getPaymentId());
            } else if (ChannelStatusEnum.CHANNEL_FAILED.equals(channelStatus)
                    || ChannelStatusEnum.CALLBACK_FAILED.equals(channelStatus)) {
                freshOrder.setStatus(PaymentStatusEnum.FAILED.getCode());
                log.info("渠道回调失败, paymentId: {}, 开始冲正", dto.getPaymentId());
                if (PaymentTypeEnum.WITHDRAW.getCode().equals(freshOrder.getPaymentType())) {
                    reverseWithdrawTransaction(freshOrder);
                }
            }

            paymentOrderMapper.updateById(freshOrder);

            PaymentOrderVO vo = convertToVO(freshOrder);

            sendChannelCallbackEvent(vo);

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

    private void validateRechargeParam(RechargeDTO dto) {
        if (CurrencyEnum.getByCode(dto.getCurrency()) == null) {
            throw new BusinessException(ResultCodeEnum.CURRENCY_NOT_SUPPORTED);
        }
        AmountUtil.validateAmount(dto.getAmount());
    }

    private void validateWithdrawParam(WithdrawDTO dto) {
        if (CurrencyEnum.getByCode(dto.getCurrency()) == null) {
            throw new BusinessException(ResultCodeEnum.CURRENCY_NOT_SUPPORTED);
        }
        AmountUtil.validateAmount(dto.getAmount());
    }

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

    private void validateSufficientBalance(Account account, BigDecimal amount) {
        long amountFen = AmountUtil.yuanToFen(amount);
        if (account.getBalance() < amountFen) {
            throw new BusinessException(ResultCodeEnum.INSUFFICIENT_BALANCE,
                    "账户余额不足, accountId: " + account.getAccountId() +
                            ", 余额: " + AmountUtil.fenToYuan(account.getBalance()) +
                            ", 需要: " + amount);
        }
    }

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

    private void createDepositTransaction(RechargeDTO dto, Account account, String paymentId) {
        TransactionCreateDTO txDTO = new TransactionCreateDTO();
        txDTO.setRequestId(dto.getRequestId());
        txDTO.setBusinessNo(dto.getBusinessNo() + "_TX");
        txDTO.setTransactionType(TransactionTypeEnum.DEPOSIT.getCode());
        txDTO.setCurrency(dto.getCurrency());
        txDTO.setTotalAmount(dto.getAmount());
        txDTO.setSummary(dto.getRemark() != null ? dto.getRemark() : "账户充值");
        txDTO.setOperator(dto.getOperator());

        List<TransactionEntryDTO> entries = new ArrayList<>();
        TransactionEntryDTO debitEntry = new TransactionEntryDTO();
        debitEntry.setAccountId(account.getAccountId());
        debitEntry.setSubjectCode("1001");
        debitEntry.setSubjectName("银行存款");
        debitEntry.setDirection(DebitCreditEnum.DEBIT.getCode());
        debitEntry.setAmount(dto.getAmount());
        debitEntry.setSummary("充值入账");
        entries.add(debitEntry);

        TransactionEntryDTO creditEntry = new TransactionEntryDTO();
        creditEntry.setAccountId(account.getAccountId());
        creditEntry.setSubjectCode("4001");
        creditEntry.setSubjectName("实收资本");
        creditEntry.setDirection(DebitCreditEnum.CREDIT.getCode());
        creditEntry.setAmount(dto.getAmount());
        creditEntry.setSummary("充值来源");
        entries.add(creditEntry);

        txDTO.setEntries(entries);

        transactionService.createTransaction(txDTO);
        log.info("充值记账完成, paymentId: {}, businessNo: {}", paymentId, dto.getBusinessNo());
    }

    private void createWithdrawTransaction(WithdrawDTO dto, Account account, String paymentId) {
        TransactionCreateDTO txDTO = new TransactionCreateDTO();
        txDTO.setRequestId(dto.getRequestId());
        txDTO.setBusinessNo(dto.getBusinessNo() + "_TX");
        txDTO.setTransactionType(TransactionTypeEnum.WITHDRAW.getCode());
        txDTO.setCurrency(dto.getCurrency());
        txDTO.setTotalAmount(dto.getAmount());
        txDTO.setSummary(dto.getRemark() != null ? dto.getRemark() : "账户提现");
        txDTO.setOperator(dto.getOperator());

        List<TransactionEntryDTO> entries = new ArrayList<>();
        TransactionEntryDTO debitEntry = new TransactionEntryDTO();
        debitEntry.setAccountId(account.getAccountId());
        debitEntry.setSubjectCode("4001");
        debitEntry.setSubjectName("实收资本");
        debitEntry.setDirection(DebitCreditEnum.DEBIT.getCode());
        debitEntry.setAmount(dto.getAmount());
        debitEntry.setSummary("提现转出");
        entries.add(debitEntry);

        TransactionEntryDTO creditEntry = new TransactionEntryDTO();
        creditEntry.setAccountId(account.getAccountId());
        creditEntry.setSubjectCode("1001");
        creditEntry.setSubjectName("银行存款");
        creditEntry.setDirection(DebitCreditEnum.CREDIT.getCode());
        creditEntry.setAmount(dto.getAmount());
        creditEntry.setSummary("提现扣减");
        entries.add(creditEntry);

        txDTO.setEntries(entries);

        transactionService.createTransaction(txDTO);
        log.info("提现记账完成, paymentId: {}, businessNo: {}", paymentId, dto.getBusinessNo());
    }

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

            List<TransactionEntryDTO> entries = new ArrayList<>();
            TransactionEntryDTO debitEntry = new TransactionEntryDTO();
            debitEntry.setAccountId(order.getAccountId());
            debitEntry.setSubjectCode("1001");
            debitEntry.setSubjectName("银行存款");
            debitEntry.setDirection(DebitCreditEnum.DEBIT.getCode());
            debitEntry.setAmount(order.getAmount());
            debitEntry.setSummary("提现冲正-加回");
            entries.add(debitEntry);

            TransactionEntryDTO creditEntry = new TransactionEntryDTO();
            creditEntry.setAccountId(order.getAccountId());
            creditEntry.setSubjectCode("4001");
            creditEntry.setSubjectName("实收资本");
            creditEntry.setDirection(DebitCreditEnum.CREDIT.getCode());
            creditEntry.setAmount(order.getAmount());
            creditEntry.setSummary("提现冲正-加回");
            entries.add(creditEntry);

            txDTO.setEntries(entries);

            transactionService.createTransaction(txDTO);
            log.info("提现冲正完成, paymentId: {}", order.getPaymentId());
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

    private void sendChannelNotification(PaymentOrder order, ChannelStatusEnum status) {
        try {
            log.info("通知外部渠道, paymentId: {}, channel: {}, status: {}",
                    order.getPaymentId(), order.getChannelCode(), status.getDesc());
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

    private void deleteAccountCache(String accountId) {
        String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();
    }
}
