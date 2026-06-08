package com.bank.core.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.AccountFreezeLog;
import com.bank.core.account.mapper.AccountFreezeLogMapper;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.service.AccountService;
import com.bank.core.api.dto.AccountCloseDTO;
import com.bank.core.api.dto.AccountCreateDTO;
import com.bank.core.api.dto.AccountFreezeDTO;
import com.bank.core.api.dto.AccountUnfreezeDTO;
import com.bank.core.api.event.AccountEvent;
import com.bank.core.api.vo.AccountVO;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final AccountFreezeLogMapper freezeLogMapper;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountVO createAccount(AccountCreateDTO dto) {
        log.info("创建账户开始, userId: {}, accountType: {}, currency: {}",
                dto.getUserId(), dto.getAccountType(), dto.getCurrency());

        IdempotentUtil.checkIdempotent(dto.getRequestId());

        validateCreateParam(dto);

        String lockKey = CommonConstants.ACCOUNT_LOCK_PREFIX + dto.getUserId() + ":"
                + dto.getAccountType() + ":" + dto.getCurrency();

        return DistributedLockUtil.executeWithLock(lockKey, () -> {
            Account existAccount = accountMapper.selectByUserIdAndTypeAndCurrency(
                    dto.getUserId(), dto.getAccountType(), dto.getCurrency());
            if (existAccount != null) {
                throw new BusinessException(ResultCodeEnum.ACCOUNT_ALREADY_EXIST);
            }

            String accountId = SnowflakeIdGenerator.generateAccountId();
            String accountNo = SnowflakeIdGenerator.generateAccountNo();

            Account account = new Account();
            account.setId(SnowflakeIdGenerator.nextId());
            account.setAccountId(accountId);
            account.setAccountNo(accountNo);
            account.setUserId(dto.getUserId());
            account.setAccountType(dto.getAccountType());
            account.setCurrency(dto.getCurrency());
            account.setBalance(AmountUtil.yuanToFen(dto.getInitBalance()));
            account.setStatus(AccountStatusEnum.NORMAL.getCode());
            account.setFreezeType(null);
            account.setOpenTime(LocalDateTime.now());
            account.setCloseTime(null);
            account.setCreateTime(LocalDateTime.now());
            account.setUpdateTime(LocalDateTime.now());
            account.setDeleted(0);

            accountMapper.insert(account);

            AccountVO accountVO = convertToVO(account);

            sendAccountEvent(accountVO, CommonConstants.ROCKETMQ_TAG_ACCOUNT_CREATE,
                    null, AccountStatusEnum.NORMAL.getCode(), dto.getRequestId(), null);

            deleteAccountCache(accountId);

            log.info("创建账户成功, accountId: {}, accountNo: {}", accountId, accountVO.getAccountNo());
            return accountVO;
        });
    }

    @Override
    public AccountVO getAccount(String accountId) {
        log.debug("查询账户信息, accountId: {}", accountId);

        String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + accountId;
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cacheValue = cacheBucket.get();
        if (cacheValue != null) {
            AccountVO accountVO = JSON.parseObject(cacheValue, AccountVO.class);
            log.debug("从缓存获取账户信息成功, accountId: {}", accountId);
            return accountVO;
        }

        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        AccountVO accountVO = convertToVO(account);
        cacheBucket.set(JSON.toJSONString(accountVO), 5, TimeUnit.MINUTES);

        return accountVO;
    }

    @Override
    public AccountVO getAccountByNo(String accountNo) {
        log.debug("根据账号查询账户, accountNo: {}", accountNo);

        Account account = accountMapper.selectByAccountNo(accountNo);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        return convertToVO(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountVO freezeAccount(AccountFreezeDTO dto) {
        log.info("冻结账户开始, accountId: {}, freezeType: {}", dto.getAccountId(), dto.getFreezeType());

        IdempotentUtil.checkIdempotent(dto.getRequestId());

        Account account = validateAccountExists(dto.getAccountId());
        if (AccountStatusEnum.FROZEN.getCode().equals(account.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_FROZEN);
        }
        if (AccountStatusEnum.CLOSED.getCode().equals(account.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_CLOSED);
        }

        if (FreezeTypeEnum.getByCode(dto.getFreezeType()) == null) {
            throw new BusinessException(ResultCodeEnum.FREEZE_TYPE_NOT_SUPPORTED);
        }

        String lockKey = CommonConstants.ACCOUNT_LOCK_PREFIX + dto.getAccountId();

        return DistributedLockUtil.executeWithLock(lockKey, () -> {
            account.setStatus(AccountStatusEnum.FROZEN.getCode());
            account.setFreezeType(dto.getFreezeType());
            account.setUpdateTime(LocalDateTime.now());
            accountMapper.updateById(account);

            recordFreezeLog(dto.getAccountId(), 1, dto.getFreezeType(),
                    dto.getRemark(), dto.getOperator());

            AccountVO accountVO = convertToVO(account);

            sendAccountEvent(accountVO, CommonConstants.ROCKETMQ_TAG_ACCOUNT_FREEZE,
                    AccountStatusEnum.NORMAL.getCode(), AccountStatusEnum.FROZEN.getCode(),
                    dto.getRequestId(), dto.getOperator());

            deleteAccountCache(dto.getAccountId());

            log.info("冻结账户成功, accountId: {}", dto.getAccountId());
            return accountVO;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountVO unfreezeAccount(AccountUnfreezeDTO dto) {
        log.info("解冻账户开始, accountId: {}, freezeType: {}", dto.getAccountId(), dto.getFreezeType());

        IdempotentUtil.checkIdempotent(dto.getRequestId());

        Account account = validateAccountExists(dto.getAccountId());
        if (!AccountStatusEnum.FROZEN.getCode().equals(account.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_FROZEN);
        }
        if (account.getFreezeType() != null && !account.getFreezeType().equals(dto.getFreezeType())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "冻结类型不匹配");
        }

        String lockKey = CommonConstants.ACCOUNT_LOCK_PREFIX + dto.getAccountId();

        return DistributedLockUtil.executeWithLock(lockKey, () -> {
            account.setStatus(AccountStatusEnum.NORMAL.getCode());
            account.setFreezeType(null);
            account.setFreezeRemark(null);
            account.setFreezeTime(null);
            account.setFreezeOperator(null);
            account.setUpdateTime(LocalDateTime.now());
            accountMapper.updateById(account);

            recordFreezeLog(dto.getAccountId(), 2, dto.getFreezeType(),
                    dto.getRemark(), dto.getOperator());

            AccountVO accountVO = convertToVO(account);

            sendAccountEvent(accountVO, CommonConstants.ROCKETMQ_TAG_ACCOUNT_UNFREEZE,
                    AccountStatusEnum.FROZEN.getCode(), AccountStatusEnum.NORMAL.getCode(),
                    dto.getRequestId(), dto.getOperator());

            deleteAccountCache(dto.getAccountId());

            log.info("解冻账户成功, accountId: {}", dto.getAccountId());
            return accountVO;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeAccount(AccountCloseDTO dto) {
        log.info("销户开始, accountId: {}", dto.getAccountId());

        IdempotentUtil.checkIdempotent(dto.getRequestId());

        Account account = validateAccountExists(dto.getAccountId());
        if (AccountStatusEnum.CLOSED.getCode().equals(account.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_CLOSED);
        }
        if (account.getBalance() > 0) {
            throw new BusinessException(ResultCodeEnum.INSUFFICIENT_BALANCE, "账户余额不为零，无法销户");
        }

        String lockKey = CommonConstants.ACCOUNT_LOCK_PREFIX + dto.getAccountId();

        DistributedLockUtil.executeWithLock(lockKey, () -> {
            account.setStatus(AccountStatusEnum.CLOSED.getCode());
            account.setFreezeType(null);
            account.setBalance(0L);
            account.setCloseTime(LocalDateTime.now());
            account.setUpdateTime(LocalDateTime.now());
            account.setDeleted(1);
            accountMapper.updateById(account);

            AccountVO accountVO = convertToVO(account);

            sendAccountEvent(accountVO, CommonConstants.ROCKETMQ_TAG_ACCOUNT_CLOSE,
                    account.getStatus(), AccountStatusEnum.CLOSED.getCode(),
                    dto.getRequestId(), dto.getOperator());

            deleteAccountCache(dto.getAccountId());

            log.info("销户成功, accountId: {}", dto.getAccountId());
            return null;
        });
    }

    private void validateCreateParam(AccountCreateDTO dto) {
        if (AccountTypeEnum.getByCode(dto.getAccountType()) == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_TYPE_NOT_SUPPORTED);
        }
        if (CurrencyEnum.getByCode(dto.getCurrency()) == null) {
            throw new BusinessException(ResultCodeEnum.CURRENCY_NOT_SUPPORTED);
        }
        AmountUtil.validateAmount(dto.getInitBalance());
    }

    private Account validateAccountExists(String accountId) {
        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }
        return account;
    }

    private AccountVO convertToVO(Account account) {
        if (account == null) {
            return null;
        }
        AccountVO vo = new AccountVO();
        BeanUtils.copyProperties(account, vo);
        vo.setBalance(AmountUtil.fenToYuan(account.getBalance()));

        AccountTypeEnum typeEnum = AccountTypeEnum.getByCode(account.getAccountType());
        if (typeEnum != null) {
            vo.setAccountTypeDesc(typeEnum.getDesc());
        }

        CurrencyEnum currencyEnum = CurrencyEnum.getByCode(account.getCurrency());
        if (currencyEnum != null) {
            vo.setCurrencyDesc(currencyEnum.getDesc());
        }

        AccountStatusEnum statusEnum = AccountStatusEnum.getByCode(account.getStatus());
        if (statusEnum != null) {
            vo.setStatusDesc(statusEnum.getDesc());
        }

        if (account.getFreezeType() != null) {
            FreezeTypeEnum freezeTypeEnum = FreezeTypeEnum.getByCode(account.getFreezeType());
            if (freezeTypeEnum != null) {
                vo.setFreezeTypeDesc(freezeTypeEnum.getDesc());
            }
        }

        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordFreezeLog(String accountId, Integer operateType, Integer freezeType,
                                String remark, String operator) {
        AccountFreezeLog log = new AccountFreezeLog();
        log.setId(SnowflakeIdGenerator.nextId());
        log.setLogId(SnowflakeIdGenerator.nextIdStr());
        log.setAccountId(accountId);
        log.setOperateType(operateType);
        log.setFreezeType(freezeType);
        log.setRemark(remark);
        log.setOperator(operator);
        log.setOperateTime(LocalDateTime.now());
        log.setCreateTime(LocalDateTime.now());
        freezeLogMapper.insert(log);
    }

    private void sendAccountEvent(AccountVO accountVO, String tag, Integer oldStatus,
                                  Integer newStatus, String requestId, String operator) {
        try {
            AccountEvent event = new AccountEvent();
            event.setEventId(SnowflakeIdGenerator.nextIdStr());
            event.setEventType(tag);
            event.setAccountId(accountVO.getAccountId());
            event.setAccountNo(accountVO.getAccountNo());
            event.setUserId(accountVO.getUserId());
            event.setAccountType(accountVO.getAccountType());
            event.setCurrency(accountVO.getCurrency());
            event.setBalance(accountVO.getBalance());
            event.setOldStatus(oldStatus);
            event.setNewStatus(newStatus);
            event.setFreezeType(accountVO.getFreezeType());
            event.setOperator(operator);
            event.setRequestId(requestId);
            event.setEventTime(LocalDateTime.now());

            String destination = CommonConstants.ROCKETMQ_TOPIC_ACCOUNT + ":" + tag;
            rocketMQTemplate.send(destination, MessageBuilder.withPayload(event).build());

            log.info("发送账户事件成功, eventId: {}, tag: {}", event.getEventId(), tag);
        } catch (Exception e) {
            log.error("发送账户事件失败, accountId: {}, tag: {}", accountVO.getAccountId(), tag, e);
        }
    }

    @Override
    public BigDecimal getBalanceCache(String accountId) {
        log.debug("查询账户余额（缓存优先）, accountId: {}", accountId);

        String balanceCacheKey = CommonConstants.ACCOUNT_BALANCE_CACHE_PREFIX + accountId;
        RBucket<String> balanceCacheBucket = redissonClient.getBucket(balanceCacheKey);
        String cachedBalance = balanceCacheBucket.get();
        if (cachedBalance != null) {
            log.debug("从余额缓存获取成功, accountId: {}, balance: {}", accountId, cachedBalance);
            return new BigDecimal(cachedBalance);
        }

        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        BigDecimal balanceYuan = AmountUtil.fenToYuan(account.getBalance());
        balanceCacheBucket.set(balanceYuan.toPlainString(),
                CommonConstants.ACCOUNT_BALANCE_CACHE_TTL, TimeUnit.SECONDS);

        log.debug("从数据库查询余额并写入缓存, accountId: {}, balance: {}", accountId, balanceYuan);
        return balanceYuan;
    }

    @Override
    public void deleteBalanceCache(String accountId) {
        String balanceCacheKey = CommonConstants.ACCOUNT_BALANCE_CACHE_PREFIX + accountId;
        redissonClient.getBucket(balanceCacheKey).delete();
        log.debug("删除账户余额缓存, accountId: {}", accountId);
    }

    private void deleteAccountCache(String accountId) {
        String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();
        deleteBalanceCache(accountId);
    }
}
