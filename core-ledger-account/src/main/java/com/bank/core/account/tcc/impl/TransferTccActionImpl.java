package com.bank.core.account.tcc.impl;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.TransferOrder;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.mapper.TransferOrderMapper;
import com.bank.core.account.tcc.TransferTccAction;
import com.bank.core.account.tcc.TccTransactionContext;
import com.bank.core.api.dto.TransferDTO;
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
public class TransferTccActionImpl implements TransferTccAction {

    private final AccountMapper accountMapper;
    private final TransferOrderMapper transferOrderMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryTransfer(BusinessActionContext context, TransferDTO dto) {
        String xid = RootContext.getXID();
        String businessNo = dto.getBusinessNo();
        
        log.info("TCC Try阶段-转账, xid={}, businessNo={}, from={}, to={}, amount={}",
                xid, businessNo, dto.getFromAccountId(), dto.getToAccountId(), dto.getAmount());

        String tryLockKey = "tcc:try:lock:" + businessNo;
        RBucket<Boolean> tryLockBucket = redissonClient.getBucket(tryLockKey);
        if (!tryLockBucket.setIfAbsent(true, 5, TimeUnit.MINUTES)) {
            log.warn("TCC Try阶段-幂等命中, businessNo={}", businessNo);
            return true;
        }

        Account fromAccount = accountMapper.selectByAccountId(dto.getFromAccountId());
        Account toAccount = accountMapper.selectByAccountId(dto.getToAccountId());

        if (fromAccount == null || toAccount == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        if (AccountStatusEnum.FROZEN.getCode().equals(fromAccount.getStatus())
                || AccountStatusEnum.CLOSED.getCode().equals(fromAccount.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_STATUS_ERROR, "付款账户状态异常");
        }

        if (AccountStatusEnum.CLOSED.getCode().equals(toAccount.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_STATUS_ERROR, "收款账户已销户");
        }

        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new BusinessException(ResultCodeEnum.TRANSFER_CURRENCY_MISMATCH);
        }

        long amountFen = AmountUtil.yuanToFen(dto.getAmount());
        
        Account freshFrom = accountMapper.selectByAccountId(dto.getFromAccountId());
        if (freshFrom.getBalance() < amountFen) {
            throw new BusinessException(ResultCodeEnum.INSUFFICIENT_BALANCE);
        }

        int updated = accountMapper.freezeBalance(
                dto.getFromAccountId(),
                amountFen,
                freshFrom.getVersion(),
                LocalDateTime.now()
        );

        if (updated == 0) {
            throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED, "账户资金冻结失败");
        }

        String transferId = SnowflakeIdGenerator.nextIdStr();
        String transferNo = SnowflakeIdGenerator.generateTransferNo();

        TransferOrder order = new TransferOrder();
        order.setId(SnowflakeIdGenerator.nextId());
        order.setTransferId(transferId);
        order.setTransferNo(transferNo);
        order.setBusinessNo(businessNo);
        order.setRequestId(dto.getRequestId());
        order.setFromAccountId(dto.getFromAccountId());
        order.setFromAccountNo(fromAccount.getAccountNo());
        order.setToAccountId(dto.getToAccountId());
        order.setToAccountNo(toAccount.getAccountNo());
        order.setAmount(dto.getAmount());
        order.setCurrency(dto.getCurrency());
        order.setStatus(PaymentStatusEnum.FROZEN.getCode());
        order.setRemark(dto.getRemark());
        order.setOperator(dto.getOperator());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setDeleted(0);
        transferOrderMapper.insert(order);

        TccTransactionContext ctx = new TccTransactionContext();
        ctx.setXid(xid);
        ctx.setBusinessNo(businessNo);
        ctx.setFromAccountId(dto.getFromAccountId());
        ctx.setToAccountId(dto.getToAccountId());
        ctx.setAmount(dto.getAmount());
        ctx.setAmountFen(amountFen);
        ctx.setCurrency(dto.getCurrency());
        ctx.setTransferId(transferId);
        ctx.setPhase(TransactionPhaseEnum.TRY.getCode());
        ctx.setCreateTime(LocalDateTime.now());

        String contextKey = "tcc:context:" + xid + ":" + businessNo;
        redissonClient.getBucket(contextKey).set(JSON.toJSONString(ctx), 24, TimeUnit.HOURS);

        context.getActionContext().put("contextKey", contextKey);
        context.getActionContext().put("transferId", transferId);
        context.getActionContext().put("amountFen", amountFen);

        log.info("TCC Try阶段-转账完成, xid={}, transferId={}, 冻结金额={}分", xid, transferId, amountFen);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(BusinessActionContext context) {
        String xid = context.getXid();
        String transferId = (String) context.getActionContext().get("transferId");
        
        log.info("TCC Confirm阶段-转账, xid={}, transferId={}", xid, transferId);

        String confirmLockKey = "tcc:confirm:lock:" + transferId;
        RBucket<Boolean> confirmLockBucket = redissonClient.getBucket(confirmLockKey);
        if (!confirmLockBucket.setIfAbsent(true, 5, TimeUnit.MINUTES)) {
            log.warn("TCC Confirm阶段-幂等命中, transferId={}", transferId);
            return true;
        }

        String contextKey = (String) context.getActionContext().get("contextKey");
        Long amountFen = (Long) context.getActionContext().get("amountFen");

        String contextJson = (String) redissonClient.getBucket(contextKey).get();
        TccTransactionContext ctx = JSON.parseObject(contextJson, TccTransactionContext.class);

        if (ctx == null) {
            log.error("TCC Confirm阶段-上下文不存在, xid={}", xid);
            return false;
        }

        TransferOrder order = transferOrderMapper.selectByTransferId(transferId);
        if (order == null) {
            log.error("TCC Confirm阶段-转账订单不存在, transferId={}", transferId);
            return false;
        }

        if (PaymentStatusEnum.SUCCESS.getCode().equals(order.getStatus())) {
            log.warn("TCC Confirm阶段-订单已处理, transferId={}", transferId);
            return true;
        }

        Account toAccount = accountMapper.selectByAccountId(ctx.getToAccountId());
        if (toAccount == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST, "收款账户不存在");
        }

        int toUpdated = accountMapper.updateBalanceWithVersion(
                ctx.getToAccountId(),
                amountFen,
                toAccount.getVersion(),
                LocalDateTime.now()
        );

        if (toUpdated == 0) {
            throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED, "收款账户入账失败");
        }

        int fromUpdated = accountMapper.unfreezeAndDeductBalance(
                ctx.getFromAccountId(),
                amountFen,
                LocalDateTime.now()
        );

        if (fromUpdated == 0) {
            throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED, "付款账户解冻扣款失败");
        }

        order.setStatus(PaymentStatusEnum.SUCCESS.getCode());
        order.setTransferTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        transferOrderMapper.updateById(order);

        deleteAccountCache(ctx.getFromAccountId());
        deleteAccountCache(ctx.getToAccountId());

        log.info("TCC Confirm阶段-转账完成, xid={}, transferId={}", xid, transferId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(BusinessActionContext context) {
        String xid = context.getXid();
        String transferId = (String) context.getActionContext().get("transferId");
        
        log.info("TCC Cancel阶段-转账回滚, xid={}, transferId={}", xid, transferId);

        String cancelLockKey = "tcc:cancel:lock:" + transferId;
        RBucket<Boolean> cancelLockBucket = redissonClient.getBucket(cancelLockKey);
        if (!cancelLockBucket.setIfAbsent(true, 5, TimeUnit.MINUTES)) {
            log.warn("TCC Cancel阶段-幂等命中, transferId={}", transferId);
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

        if (transferId != null) {
            TransferOrder order = transferOrderMapper.selectByTransferId(transferId);
            if (order != null && !PaymentStatusEnum.FAILED.getCode().equals(order.getStatus())) {
                order.setStatus(PaymentStatusEnum.FAILED.getCode());
                order.setUpdateTime(LocalDateTime.now());
                transferOrderMapper.updateById(order);
            }
        }

        int unfreezeUpdated = accountMapper.unfreezeBalance(
                ctx.getFromAccountId(),
                amountFen,
                LocalDateTime.now()
        );

        if (unfreezeUpdated == 0) {
            log.warn("TCC Cancel阶段-解冻失败, 可能资金已解冻, accountId={}, amount={}",
                    ctx.getFromAccountId(), amountFen);
        }

        deleteAccountCache(ctx.getFromAccountId());

        log.info("TCC Cancel阶段-转账回滚完成, xid={}, transferId={}", xid, transferId);
        return true;
    }

    private void deleteAccountCache(String accountId) {
        String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();
    }
}
