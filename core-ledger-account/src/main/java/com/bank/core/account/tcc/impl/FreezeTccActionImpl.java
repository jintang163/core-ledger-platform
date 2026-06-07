package com.bank.core.account.tcc.impl;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.AccountFreezeLog;
import com.bank.core.account.mapper.AccountFreezeLogMapper;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.tcc.FreezeTccAction;
import com.bank.core.account.tcc.TccTransactionContext;
import com.bank.core.api.dto.AccountFreezeDTO;
import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.*;
import com.bank.core.common.exception.BusinessException;
import com.bank.core.common.utils.AmountUtil;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FreezeTccActionImpl implements FreezeTccAction {

    private final AccountMapper accountMapper;
    private final AccountFreezeLogMapper freezeLogMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryFreeze(BusinessActionContext context, AccountFreezeDTO dto) {
        String xid = RootContext.getXID();
        String businessNo = dto.getBusinessNo() != null ? dto.getBusinessNo() : dto.getRequestId();
        
        log.info("TCC Try阶段-冻结, xid={}, accountId={}, freezeType={}, amount={}",
                xid, dto.getAccountId(), dto.getFreezeType(), dto.getAmount());

        String tryLockKey = "tcc:freeze:try:lock:" + businessNo;
        RBucket<Boolean> tryLockBucket = redissonClient.getBucket(tryLockKey);
        if (!tryLockBucket.setIfAbsent(true, 5, TimeUnit.MINUTES)) {
            log.warn("TCC Try阶段-冻结幂等命中, businessNo={}", businessNo);
            return true;
        }

        Account account = accountMapper.selectByAccountId(dto.getAccountId());
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        if (AccountStatusEnum.CLOSED.getCode().equals(account.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_CLOSED, "账户已销户，无法冻结");
        }

        if (FreezeTypeEnum.getByCode(dto.getFreezeType()) == null) {
            throw new BusinessException(ResultCodeEnum.FREEZE_TYPE_NOT_SUPPORTED);
        }

        Long amountFen = 0L;
        if (dto.getAmount() != null) {
            amountFen = AmountUtil.yuanToFen(dto.getAmount());
            Account freshAccount = accountMapper.selectByAccountId(dto.getAccountId());
            if (freshAccount.getBalance() < amountFen) {
                throw new BusinessException(ResultCodeEnum.INSUFFICIENT_BALANCE, "可冻结余额不足");
            }

            int updated = accountMapper.freezeBalance(
                    dto.getAccountId(),
                    amountFen,
                    freshAccount.getVersion(),
                    LocalDateTime.now()
            );

            if (updated == 0) {
                throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED, "资金冻结失败");
            }
        }

        String freezeLogId = SnowflakeIdGenerator.nextIdStr();
        AccountFreezeLog freezeLog = new AccountFreezeLog();
        freezeLog.setId(SnowflakeIdGenerator.nextId());
        freezeLog.setLogId(freezeLogId);
        freezeLog.setAccountId(dto.getAccountId());
        freezeLog.setOperateType(1);
        freezeLog.setFreezeType(dto.getFreezeType());
        freezeLog.setRemark(dto.getRemark());
        freezeLog.setOperator(dto.getOperator());
        freezeLog.setOperateTime(LocalDateTime.now());
        freezeLog.setCreateTime(LocalDateTime.now());
        freezeLogMapper.insert(freezeLog);

        TccTransactionContext ctx = new TccTransactionContext();
        ctx.setXid(xid);
        ctx.setBusinessNo(businessNo);
        ctx.setAccountId(dto.getAccountId());
        ctx.setAmount(dto.getAmount());
        ctx.setAmountFen(amountFen);
        ctx.setCurrency(dto.getCurrency());
        ctx.setFreezeType(dto.getFreezeType());
        ctx.setFreezeLogId(freezeLogId);
        ctx.setPhase(TransactionPhaseEnum.TRY.getCode());
        ctx.setCreateTime(LocalDateTime.now());

        String contextKey = "tcc:freeze:context:" + xid + ":" + businessNo;
        redissonClient.getBucket(contextKey).set(JSON.toJSONString(ctx), 24, TimeUnit.HOURS);

        context.getActionContext().put("contextKey", contextKey);
        context.getActionContext().put("freezeLogId", freezeLogId);
        context.getActionContext().put("amountFen", amountFen);

        log.info("TCC Try阶段-冻结完成, xid={}, freezeLogId={}, 冻结金额={}分", xid, freezeLogId, amountFen);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(BusinessActionContext context) {
        String xid = context.getXid();
        String freezeLogId = (String) context.getActionContext().get("freezeLogId");
        
        log.info("TCC Confirm阶段-冻结, xid={}, freezeLogId={}", xid, freezeLogId);

        String confirmLockKey = "tcc:freeze:confirm:lock:" + freezeLogId;
        RBucket<Boolean> confirmLockBucket = redissonClient.getBucket(confirmLockKey);
        if (!confirmLockBucket.setIfAbsent(true, 5, TimeUnit.MINUTES)) {
            log.warn("TCC Confirm阶段-冻结幂等命中, freezeLogId={}", freezeLogId);
            return true;
        }

        String contextKey = (String) context.getActionContext().get("contextKey");
        String contextJson = (String) redissonClient.getBucket(contextKey).get();
        TccTransactionContext ctx = JSON.parseObject(contextJson, TccTransactionContext.class);

        if (ctx == null) {
            log.error("TCC Confirm阶段-上下文不存在, xid={}", xid);
            return false;
        }

        Account account = accountMapper.selectByAccountId(ctx.getAccountId());
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        accountMapper.freezeAccount(
                ctx.getAccountId(),
                AccountStatusEnum.FROZEN.getCode(),
                ctx.getFreezeType(),
                ctx.getBusinessNo(),
                LocalDateTime.now(),
                ctx.getFreezeType() != null ? String.valueOf(ctx.getFreezeType()) : "system",
                LocalDateTime.now()
        );

        deleteAccountCache(ctx.getAccountId());

        log.info("TCC Confirm阶段-冻结完成, xid={}, accountId={}", xid, ctx.getAccountId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(BusinessActionContext context) {
        String xid = context.getXid();
        String freezeLogId = (String) context.getActionContext().get("freezeLogId");
        
        log.info("TCC Cancel阶段-冻结回滚, xid={}, freezeLogId={}", xid, freezeLogId);

        String cancelLockKey = "tcc:freeze:cancel:lock:" + freezeLogId;
        RBucket<Boolean> cancelLockBucket = redissonClient.getBucket(cancelLockKey);
        if (!cancelLockBucket.setIfAbsent(true, 5, TimeUnit.MINUTES)) {
            log.warn("TCC Cancel阶段-冻结幂等命中, freezeLogId={}", freezeLogId);
            return true;
        }

        String contextKey = (String) context.getActionContext().get("contextKey");
        Long amountFen = (Long) context.getActionContext().get("amountFen");

        String contextJson = (String) redissonClient.getBucket(contextKey).get();
        if (contextJson == null) {
            log.warn("TCC Cancel阶段-上下文不存在, 可能Try阶段未执行成功, xid={}", xid);
            return true;
        }

        TccTransactionContext ctx = JSON.parseObject(contextJson, TccTransactionContext.class);

        AccountFreezeLog freezeLog = new AccountFreezeLog();
        freezeLog.setId(SnowflakeIdGenerator.nextId());
        freezeLog.setLogId(SnowflakeIdGenerator.nextIdStr());
        freezeLog.setAccountId(ctx.getAccountId());
        freezeLog.setOperateType(2);
        freezeLog.setFreezeType(ctx.getFreezeType());
        freezeLog.setRemark("TCC事务回滚-解冻");
        freezeLog.setOperator("system");
        freezeLog.setOperateTime(LocalDateTime.now());
        freezeLog.setCreateTime(LocalDateTime.now());
        freezeLogMapper.insert(freezeLog);

        if (amountFen != null && amountFen > 0) {
            int unfreezeUpdated = accountMapper.unfreezeBalance(
                    ctx.getAccountId(),
                    amountFen,
                    LocalDateTime.now()
            );

            if (unfreezeUpdated == 0) {
                log.warn("TCC Cancel阶段-解冻金额失败, 可能资金已解冻, accountId={}, amount={}",
                        ctx.getAccountId(), amountFen);
            }
        }

        deleteAccountCache(ctx.getAccountId());

        log.info("TCC Cancel阶段-冻结回滚完成, xid={}, accountId={}", xid, ctx.getAccountId());
        return true;
    }

    private void deleteAccountCache(String accountId) {
        String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();
    }
}
