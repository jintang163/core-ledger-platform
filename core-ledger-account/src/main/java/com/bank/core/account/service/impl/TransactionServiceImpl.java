package com.bank.core.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.config.HotAccountConfig;
import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.AccountShard;
import com.bank.core.account.entity.Transaction;
import com.bank.core.account.entity.TransactionEntry;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.mapper.TransactionEntryMapper;
import com.bank.core.account.mapper.TransactionMapper;
import com.bank.core.account.service.AccountLockService;
import com.bank.core.account.service.BufferAccountingService;
import com.bank.core.account.service.HotAccountService;
import com.bank.core.account.service.TransactionService;
import com.bank.core.api.dto.TransactionCreateDTO;
import com.bank.core.api.dto.TransactionEntryDTO;
import com.bank.core.api.dto.TransactionQueryDTO;
import com.bank.core.api.event.AccountEvent;
import com.bank.core.api.vo.TransactionEntryVO;
import com.bank.core.api.vo.TransactionVO;
import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.*;
import com.bank.core.common.exception.BusinessException;
import com.bank.core.common.utils.AmountUtil;
import com.bank.core.common.utils.DistributedLockUtil;
import com.bank.core.common.utils.RetryUtil;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.support.MessageBuilder;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 交易服务实现类（增强版-支持高并发与热点账户处理）
 *
 * 高并发特性：
 * 1. 热点账户分片：将热点账户拆分为多个影子子账户，随机路由扣款，定期归并
 * 2. 缓冲记账：先记录流水并异步批量更新余额，适用于允许短暂弱一致的场景
 * 3. 乐观锁重试：更新余额失败时自动重试，使用指数退避策略
 * 4. 分布式锁：对同一账户操作加锁（Redis/Redisson），防止并发冲突
 *
 * 配置开关：通过HotAccountConfig配置类控制各项功能的启用/禁用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final TransactionEntryMapper entryMapper;
    private final AccountMapper accountMapper;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;

    /** 热点账户服务 - 提供分片、路由、归并功能 */
    private final HotAccountService hotAccountService;

    /** 缓冲记账服务 - 提供流水记录、异步批量更新功能 */
    private final BufferAccountingService bufferAccountingService;

    /** 账户级分布式锁服务 - 提供细粒度的账户级别锁 */
    private final AccountLockService accountLockService;

    /** 热点账户配置 - 控制各项高并发功能的启用/禁用 */
    private final HotAccountConfig hotAccountConfig;

    @Override
    @GlobalTransactional(name = "create-transaction", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public TransactionVO createTransaction(TransactionCreateDTO dto) {
        log.info("开始记账, businessNo: {}, transactionType: {}", dto.getBusinessNo(), dto.getTransactionType());

        if (dto.getRequestId() == null || dto.getRequestId().trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "请求ID不能为空");
        }

        String idempotentKey = CommonConstants.TRANSACTION_IDEMPOTENT_PREFIX + dto.getBusinessNo();
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);
        String cachedTransactionId = idempotentBucket.get();
        if (cachedTransactionId != null) {
            log.warn("幂等命中, businessNo: {}, transactionId: {}, 直接返回已有结果", dto.getBusinessNo(), cachedTransactionId);
            Transaction existTx = transactionMapper.selectByTransactionId(cachedTransactionId);
            if (existTx != null) {
                return convertToVO(existTx, true);
            }
        }

        Transaction existTx = transactionMapper.selectByBusinessNo(dto.getBusinessNo());
        if (existTx != null) {
            log.warn("业务单号已存在, businessNo: {}, 直接返回已有结果", dto.getBusinessNo());
            idempotentBucket.set(existTx.getTransactionId(), 24, TimeUnit.HOURS);
            return convertToVO(existTx, true);
        }

        validateParam(dto);

        String lockKey = CommonConstants.TRANSACTION_LOCK_PREFIX + dto.getBusinessNo();
        return DistributedLockUtil.executeWithLock(lockKey, () -> {
            Transaction existAgain = transactionMapper.selectByBusinessNo(dto.getBusinessNo());
            if (existAgain != null) {
                log.warn("分布式锁内二次检查发现业务单号已存在, businessNo: {}", dto.getBusinessNo());
                idempotentBucket.set(existAgain.getTransactionId(), 24, TimeUnit.HOURS);
                return convertToVO(existAgain, true);
            }

            validateDebitCreditBalance(dto.getEntries());

            List<Account> accounts = validateAndPrepareAccounts(dto.getEntries());

            String transactionId = SnowflakeIdGenerator.nextIdStr();
            String transactionNo = SnowflakeIdGenerator.generateTransactionNo();
            String voucherNo = SnowflakeIdGenerator.generateVoucherNo();

            Transaction transaction = buildTransaction(dto, transactionId, transactionNo, voucherNo);

            List<TransactionEntry> entries = buildEntries(dto.getEntries(), transactionId, accounts);

            updateAccountBalancesWithRetry(accounts, dto.getEntries());

            transactionMapper.insert(transaction);
            for (TransactionEntry entry : entries) {
                entryMapper.insert(entry);
            }

            transaction.setStatus(TransactionStatusEnum.SUCCESS.getCode());
            transactionMapper.updateById(transaction);

            TransactionVO vo = convertToVO(transaction, entries, accounts);

            idempotentBucket.set(transactionId, 24, TimeUnit.HOURS);

            sendTransactionEvent(vo, dto.getRequestId(), dto.getOperator());

            deleteAccountCaches(accounts);

            log.info("记账成功, transactionId: {}, transactionNo: {}", transactionId, transactionNo);
            return vo;
        });
    }

    @Override
    public TransactionVO getTransaction(String transactionId) {
        log.debug("查询交易, transactionId: {}", transactionId);

        String cacheKey = CommonConstants.TRANSACTION_CACHE_PREFIX + transactionId;
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cacheValue = cacheBucket.get();
        if (cacheValue != null) {
            TransactionVO vo = JSON.parseObject(cacheValue, TransactionVO.class);
            log.debug("从缓存获取交易成功, transactionId: {}", transactionId);
            return vo;
        }

        Transaction transaction = transactionMapper.selectByTransactionId(transactionId);
        if (transaction == null) {
            throw new BusinessException(ResultCodeEnum.TRANSACTION_NOT_EXIST);
        }

        TransactionVO vo = convertToVO(transaction, true);
        cacheBucket.set(JSON.toJSONString(vo), 30, TimeUnit.MINUTES);

        return vo;
    }

    @Override
    public TransactionVO getTransactionByBusinessNo(String businessNo) {
        log.debug("根据业务流水号查询交易, businessNo: {}", businessNo);

        Transaction transaction = transactionMapper.selectByBusinessNo(businessNo);
        if (transaction == null) {
            throw new BusinessException(ResultCodeEnum.TRANSACTION_NOT_EXIST);
        }

        return convertToVO(transaction, true);
    }

    @Override
    public Page<TransactionVO> queryTransactions(TransactionQueryDTO dto) {
        log.info("查询交易列表, accountId: {}, type: {}, status: {}",
                dto.getAccountId(), dto.getTransactionType(), dto.getStatus());

        int pageNum = Math.max(dto.getPageNum(), 1);
        int pageSize = Math.min(dto.getPageSize(), CommonConstants.MAX_PAGE_SIZE);

        Page<Transaction> page = new Page<>(pageNum, pageSize);
        Page<Transaction> resultPage = transactionMapper.selectByCondition(
                page,
                dto.getAccountId(),
                dto.getTransactionType(),
                dto.getStatus(),
                dto.getStartTime(),
                dto.getEndTime()
        );

        Page<TransactionVO> voPage = new Page<>(pageNum, pageSize, resultPage.getTotal());
        List<TransactionVO> voList = resultPage.getRecords().stream()
                .map(tx -> convertToVO(tx, false))
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    private void validateParam(TransactionCreateDTO dto) {
        if (TransactionTypeEnum.getByCode(dto.getTransactionType()) == null) {
            throw new BusinessException(ResultCodeEnum.INVALID_TRANSACTION_TYPE);
        }
        if (CurrencyEnum.getByCode(dto.getCurrency()) == null) {
            throw new BusinessException(ResultCodeEnum.CURRENCY_NOT_SUPPORTED);
        }
        if (dto.getEntries() == null || dto.getEntries().size() < 2) {
            throw new BusinessException(ResultCodeEnum.INSUFFICIENT_ENTRY);
        }
    }

    private void validateDebitCreditBalance(List<TransactionEntryDTO> entries) {
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        for (TransactionEntryDTO entry : entries) {
            DebitCreditEnum direction = DebitCreditEnum.getByCode(entry.getDirection());
            if (direction == null) {
                throw new BusinessException(ResultCodeEnum.INVALID_DIRECTION);
            }
            if (DebitCreditEnum.DEBIT.equals(direction)) {
                debitTotal = debitTotal.add(entry.getAmount());
            } else {
                creditTotal = creditTotal.add(entry.getAmount());
            }
        }

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new BusinessException(ResultCodeEnum.DEBIT_CREDIT_NOT_BALANCE,
                    "借方金额: " + debitTotal + ", 贷方金额: " + creditTotal);
        }

        log.info("借贷平衡校验通过, 借方总额: {}, 贷方总额: {}", debitTotal, creditTotal);
    }

    /**
     * 校验并准备账户
     * 增强功能：
     * 1. 热点账户：校验总可用余额（主账户 + 所有影子账户）
     * 2. 缓冲记账：校验可用余额（账户余额 + 待处理缓冲金额）
     */
    private List<Account> validateAndPrepareAccounts(List<TransactionEntryDTO> entries) {
        List<Account> accounts = new ArrayList<>();
        for (TransactionEntryDTO entry : entries) {
            Account account = accountMapper.selectByAccountId(entry.getAccountId());
            if (account == null) {
                throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST, "账户不存在: " + entry.getAccountId());
            }
            if (AccountStatusEnum.CLOSED.getCode().equals(account.getStatus())) {
                throw new BusinessException(ResultCodeEnum.ACCOUNT_CLOSED, "账户已销户: " + entry.getAccountId());
            }
            if (AccountStatusEnum.FROZEN.getCode().equals(account.getStatus())) {
                throw new BusinessException(ResultCodeEnum.ACCOUNT_FROZEN, "账户已冻结: " + entry.getAccountId());
            }

            DebitCreditEnum direction = DebitCreditEnum.getByCode(entry.getDirection());
            if (DebitCreditEnum.CREDIT.equals(direction)) {
                long amountFen = AmountUtil.yuanToFen(entry.getAmount());

                long availableBalance = account.getBalance();

                if (hotAccountConfig.getHot().isEnabled() && hotAccountConfig.getHot().isShardingEnabled()) {
                    if (hotAccountService.isHotAccount(entry.getAccountId())) {
                        availableBalance = hotAccountService.getTotalAvailableBalance(entry.getAccountId());
                        log.debug("热点账户余额校验, accountId: {}, 总可用余额: {}分", entry.getAccountId(), availableBalance);
                    }
                }

                if (hotAccountConfig.getHot().isEnabled() && hotAccountConfig.getHot().isBufferEnabled()) {
                    if (bufferAccountingService.isBufferEnabled(entry.getAccountId())) {
                        availableBalance = bufferAccountingService.getAvailableBalance(entry.getAccountId());
                        log.debug("缓冲记账账户余额校验, accountId: {}, 可用余额: {}分", entry.getAccountId(), availableBalance);
                    }
                }

                if (availableBalance < amountFen) {
                    throw new BusinessException(ResultCodeEnum.INSUFFICIENT_BALANCE,
                            "账户余额不足, accountId: " + entry.getAccountId() +
                                    ", 可用余额: " + AmountUtil.fenToYuan(availableBalance) +
                                    ", 需要: " + entry.getAmount());
                }
            }

            accounts.add(account);
        }
        return accounts;
    }

    /**
     * 更新账户余额（增强版-支持高并发与热点账户处理）
     *
     * 高并发特性集成：
     * 1. 账户级分布式锁：对涉及的所有账户加锁，防止并发冲突
     * 2. 乐观锁指数退避重试：使用RetryUtil进行带指数退避的自动重试
     * 3. 热点账户分片：热点账户的扣款操作路由到影子子账户
     *
     * 处理流程：
     * 1. 提取所有涉及的账户ID，按字典序排序后获取分布式锁（防止死锁）
     * 2. 在锁保护下执行余额更新
     * 3. 对于热点账户的扣款（贷方）操作，路由到影子子账户执行
     * 4. 余额更新使用乐观锁+版本号机制
     * 5. 乐观锁冲突时使用指数退避策略自动重试
     *
     * @param accounts 账户列表
     * @param entries 交易分录列表
     */
    private void updateAccountBalancesWithRetry(List<Account> accounts, List<TransactionEntryDTO> entries) {
        List<String> accountIds = entries.stream()
                .map(TransactionEntryDTO::getAccountId)
                .distinct()
                .toList();

        if (hotAccountConfig.getHot().isEnabled() && hotAccountConfig.getHot().isAccountLockEnabled()) {
            accountLockService.executeWithMultiAccountLock(accountIds, () -> {
                doUpdateAccountBalances(accounts, entries);
                return null;
            });
        } else {
            doUpdateAccountBalances(accounts, entries);
        }
    }

    /**
     * 执行实际的账户余额更新
     * 支持乐观锁指数退避重试和热点账户分片
     */
    private void doUpdateAccountBalances(List<Account> accounts, List<TransactionEntryDTO> entries) {
        if (hotAccountConfig.getHot().isEnabled() && hotAccountConfig.getHot().isOptimisticRetryEnabled()) {
            RetryUtil.executeWithRetry(() -> {
                updateBalancesInternal(accounts, entries);
                return null;
            });
        } else {
            updateBalancesInternal(accounts, entries);
        }
    }

    /**
     * 内部余额更新逻辑
     * 核心处理：
     * 1. 普通账户：直接更新主账户余额
     * 2. 热点账户扣款（贷方）：路由到影子子账户更新
     * 3. 热点账户入账（借方）：直接更新主账户余额
     *
     * 会计处理逻辑：
     * - 借方（DEBIT）：资产/费用增加，负债/权益减少 → 余额增加
     * - 贷方（CREDIT）：资产/费用减少，负债/权益增加 → 余额减少
     *
     * 热点账户处理：
     * - 扣款（CREDIT）：路由到影子账户，分散并发压力
     * - 入账（DEBIT）：直接入主账户，保证资金可见性
     */
    private void updateBalancesInternal(List<Account> accounts, List<TransactionEntryDTO> entries) {
        for (int i = 0; i < entries.size(); i++) {
            TransactionEntryDTO entry = entries.get(i);
            Account account = accounts.get(i);

            DebitCreditEnum direction = DebitCreditEnum.getByCode(entry.getDirection());
            long amountFen = AmountUtil.yuanToFen(entry.getAmount());
            long changeAmount = DebitCreditEnum.DEBIT.equals(direction) ? amountFen : -amountFen;

            boolean isHotShardingEnabled = hotAccountConfig.getHot().isEnabled()
                    && hotAccountConfig.getHot().isShardingEnabled()
                    && hotAccountService.isHotAccount(entry.getAccountId());

            if (isHotShardingEnabled && DebitCreditEnum.CREDIT.equals(direction)) {
                AccountShard shard = hotAccountService.routeShard(entry.getAccountId());
                log.debug("热点账户扣款路由到影子账户, accountId: {}, shardId: {}, amount: {}分",
                        entry.getAccountId(), shard.getShardId(), changeAmount);

                boolean shardUpdated = hotAccountService.updateShardBalance(shard.getShardId(), changeAmount);
                if (!shardUpdated) {
                    throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED,
                            "影子账户余额更新失败, shardId: " + shard.getShardId());
                }

                Account freshAccount = accountMapper.selectByAccountId(entry.getAccountId());
                account.setVersion(freshAccount.getVersion());
                account.setBalance(freshAccount.getBalance() + changeAmount);
            } else {
                Account freshAccount = accountMapper.selectByAccountId(entry.getAccountId());
                if (freshAccount == null) {
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
                }

                int updated = accountMapper.updateBalanceWithVersion(
                        entry.getAccountId(),
                        changeAmount,
                        freshAccount.getVersion(),
                        LocalDateTime.now()
                );

                if (updated == 0) {
                    throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED);
                }

                account.setVersion(freshAccount.getVersion() + 1);
                account.setBalance(freshAccount.getBalance() + changeAmount);
            }
        }
    }

    private Transaction buildTransaction(TransactionCreateDTO dto, String transactionId,
                                          String transactionNo, String voucherNo) {
        Transaction transaction = new Transaction();
        transaction.setId(SnowflakeIdGenerator.nextId());
        transaction.setTransactionId(transactionId);
        transaction.setTransactionNo(transactionNo);
        transaction.setTransactionType(dto.getTransactionType());
        transaction.setBusinessNo(dto.getBusinessNo());
        transaction.setTotalAmount(dto.getTotalAmount());
        transaction.setCurrency(dto.getCurrency());
        transaction.setVoucherNo(voucherNo);
        transaction.setSummary(dto.getSummary());
        transaction.setStatus(TransactionStatusEnum.PENDING.getCode());
        transaction.setRequestId(dto.getRequestId());
        transaction.setOperator(dto.getOperator());
        transaction.setTransactionTime(LocalDateTime.now());
        transaction.setCreateTime(LocalDateTime.now());
        transaction.setUpdateTime(LocalDateTime.now());
        transaction.setDeleted(0);
        return transaction;
    }

    private List<TransactionEntry> buildEntries(List<TransactionEntryDTO> entryDTOs,
                                                 String transactionId, List<Account> accounts) {
        List<TransactionEntry> entries = new ArrayList<>();
        for (int i = 0; i < entryDTOs.size(); i++) {
            TransactionEntryDTO dto = entryDTOs.get(i);
            Account account = accounts.get(i);

            TransactionEntry entry = new TransactionEntry();
            entry.setId(SnowflakeIdGenerator.nextId());
            entry.setEntryId(SnowflakeIdGenerator.generateEntryId());
            entry.setTransactionId(transactionId);
            entry.setAccountId(dto.getAccountId());
            entry.setAccountNo(account.getAccountNo());
            entry.setSubjectCode(dto.getSubjectCode());
            entry.setSubjectName(dto.getSubjectName());
            entry.setDirection(dto.getDirection());
            entry.setAmount(dto.getAmount());
            entry.setCurrency(account.getCurrency());
            entry.setSummary(dto.getSummary());
            entry.setCreateTime(LocalDateTime.now());

            entries.add(entry);
        }
        return entries;
    }

    private TransactionVO convertToVO(Transaction transaction, boolean withEntries) {
        List<TransactionEntry> entries = null;
        if (withEntries) {
            entries = entryMapper.selectByTransactionId(transaction.getTransactionId());
        }
        return convertToVO(transaction, entries, null);
    }

    private TransactionVO convertToVO(Transaction transaction, List<TransactionEntry> entries,
                                       List<Account> accounts) {
        TransactionVO vo = new TransactionVO();
        BeanUtils.copyProperties(transaction, vo);

        TransactionTypeEnum typeEnum = TransactionTypeEnum.getByCode(transaction.getTransactionType());
        if (typeEnum != null) {
            vo.setTransactionTypeDesc(typeEnum.getDesc());
        }

        TransactionStatusEnum statusEnum = TransactionStatusEnum.getByCode(transaction.getStatus());
        if (statusEnum != null) {
            vo.setStatusDesc(statusEnum.getDesc());
        }

        if (entries != null && !entries.isEmpty()) {
            List<TransactionEntryVO> entryVOs = entries.stream().map(entry -> {
                TransactionEntryVO entryVO = new TransactionEntryVO();
                BeanUtils.copyProperties(entry, entryVO);
                DebitCreditEnum direction = DebitCreditEnum.getByCode(entry.getDirection());
                if (direction != null) {
                    entryVO.setDirectionDesc(direction.getDesc());
                }
                return entryVO;
            }).collect(Collectors.toList());
            vo.setEntries(entryVOs);
        }

        return vo;
    }

    private void sendTransactionEvent(TransactionVO vo, String requestId, String operator) {
        try {
            AccountEvent event = new AccountEvent();
            event.setEventId(SnowflakeIdGenerator.nextIdStr());
            event.setEventType(CommonConstants.ROCKETMQ_TAG_TRANSACTION);
            event.setAccountId(vo.getEntries().get(0).getAccountId());
            event.setAccountNo(vo.getEntries().get(0).getAccountNo());
            event.setTransactionId(vo.getTransactionId());
            event.setTransactionType(vo.getTransactionType());
            event.setBalance(vo.getTotalAmount());
            event.setRequestId(requestId);
            event.setOperator(operator);
            event.setEventTime(LocalDateTime.now());

            String destination = CommonConstants.ROCKETMQ_TOPIC_ACCOUNT + ":"
                    + CommonConstants.ROCKETMQ_TAG_TRANSACTION;
            rocketMQTemplate.send(destination, MessageBuilder.withPayload(event).build());

            log.info("发送交易事件成功, transactionId: {}", vo.getTransactionId());
        } catch (Exception e) {
            log.error("发送交易事件失败, transactionId: {}", vo.getTransactionId(), e);
        }
    }

    /**
     * 删除账户相关缓存
     * 包括：
     * 1. 普通账户缓存
     * 2. 热点账户状态缓存
     * 3. 影子账户分片缓存
     */
    private void deleteAccountCaches(List<Account> accounts) {
        for (Account account : accounts) {
            String accountId = account.getAccountId();

            String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + accountId;
            redissonClient.getBucket(cacheKey).delete();

            String hotCacheKey = CommonConstants.HOT_ACCOUNT_LOCK_PREFIX + accountId;
            redissonClient.getBucket(hotCacheKey).delete();

            String shardCacheKey = CommonConstants.SHARD_ACCOUNT_CACHE_PREFIX + accountId;
            redissonClient.getBucket(shardCacheKey).delete();
        }
    }
}
