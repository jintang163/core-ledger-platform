package com.bank.core.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.AccountTccLog;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.mapper.AccountTccLogMapper;
import com.bank.core.api.tcc.AccountTccService;
import com.bank.core.common.enums.AccountStatusEnum;
import com.bank.core.common.utils.AmountUtil;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountTccServiceImpl implements AccountTccService {

    private final AccountMapper accountMapper;
    private final AccountTccLogMapper tccLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean prepareCreate(BusinessActionContext actionContext, String accountId,
                                 String userId, Integer accountType, String currency,
                                 BigDecimal initBalance) {
        String txId = actionContext.getXid();
        log.info("TCC Try - 创建账户, txId: {}, accountId: {}", txId, accountId);

        saveTccLog(txId, "createAccountTcc", 1, accountId, buildContext(
                "userId", userId,
                "accountType", accountType,
                "currency", currency,
                "initBalance", initBalance
        ));

        Account account = new Account();
        account.setId(SnowflakeIdGenerator.nextId());
        account.setAccountId(accountId);
        account.setAccountNo(SnowflakeIdGenerator.generateAccountNo());
        account.setUserId(userId);
        account.setAccountType(accountType);
        account.setCurrency(currency);
        account.setBalance(AmountUtil.yuanToFen(initBalance));
        account.setStatus(AccountStatusEnum.NORMAL.getCode());
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        account.setDeleted(0);

        accountMapper.insert(account);

        log.info("TCC Try - 创建账户成功, txId: {}, accountId: {}", txId, accountId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean commit(BusinessActionContext actionContext) {
        String txId = actionContext.getXid();
        log.info("TCC Confirm - 创建账户, txId: {}", txId);

        AccountTccLog existLog = tccLogMapper.selectByTxIdAndActionAndPhase(
                txId, "createAccountTcc", 2);
        if (existLog != null) {
            log.warn("TCC Confirm - 已执行过，跳过, txId: {}", txId);
            return true;
        }

        saveTccLog(txId, "createAccountTcc", 2,
                (String) actionContext.getActionContext("accountId"), null);

        log.info("TCC Confirm - 创建账户成功, txId: {}", txId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean rollback(BusinessActionContext actionContext) {
        String txId = actionContext.getXid();
        String accountId = (String) actionContext.getActionContext("accountId");
        log.info("TCC Cancel - 创建账户, txId: {}, accountId: {}", txId, accountId);

        AccountTccLog existLog = tccLogMapper.selectByTxIdAndActionAndPhase(
                txId, "createAccountTcc", 3);
        if (existLog != null) {
            log.warn("TCC Cancel - 已执行过，跳过, txId: {}", txId);
            return true;
        }

        saveTccLog(txId, "createAccountTcc", 3, accountId, null);

        if (accountId != null) {
            Account account = accountMapper.selectByAccountId(accountId);
            if (account != null) {
                accountMapper.deleteById(account.getId());
                log.info("TCC Cancel - 删除账户成功, accountId: {}", accountId);
            }
        }

        log.info("TCC Cancel - 创建账户成功, txId: {}", txId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean prepareFreeze(BusinessActionContext actionContext, String accountId,
                                 Integer freezeType, String remark, String operator) {
        String txId = actionContext.getXid();
        log.info("TCC Try - 冻结账户, txId: {}, accountId: {}", txId, accountId);

        saveTccLog(txId, "freezeAccountTcc", 1, accountId, buildContext(
                "freezeType", freezeType,
                "remark", remark,
                "operator", operator
        ));

        int rows = accountMapper.freezeAccount(
                accountId,
                AccountStatusEnum.FROZEN.getCode(),
                freezeType,
                remark,
                LocalDateTime.now(),
                operator,
                LocalDateTime.now()
        );

        if (rows <= 0) {
            log.error("TCC Try - 冻结账户失败, txId: {}, accountId: {}", txId, accountId);
            return false;
        }

        log.info("TCC Try - 冻结账户成功, txId: {}, accountId: {}", txId, accountId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean commitFreeze(BusinessActionContext actionContext) {
        String txId = actionContext.getXid();
        log.info("TCC Confirm - 冻结账户, txId: {}", txId);

        AccountTccLog existLog = tccLogMapper.selectByTxIdAndActionAndPhase(
                txId, "freezeAccountTcc", 2);
        if (existLog != null) {
            log.warn("TCC Confirm - 已执行过，跳过, txId: {}", txId);
            return true;
        }

        saveTccLog(txId, "freezeAccountTcc", 2,
                (String) actionContext.getActionContext("accountId"), null);

        log.info("TCC Confirm - 冻结账户成功, txId: {}", txId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean rollbackFreeze(BusinessActionContext actionContext) {
        String txId = actionContext.getXid();
        String accountId = (String) actionContext.getActionContext("accountId");
        log.info("TCC Cancel - 冻结账户, txId: {}, accountId: {}", txId, accountId);

        AccountTccLog existLog = tccLogMapper.selectByTxIdAndActionAndPhase(
                txId, "freezeAccountTcc", 3);
        if (existLog != null) {
            log.warn("TCC Cancel - 已执行过，跳过, txId: {}", txId);
            return true;
        }

        saveTccLog(txId, "freezeAccountTcc", 3, accountId, null);

        if (accountId != null) {
            accountMapper.unfreezeAccount(accountId, AccountStatusEnum.NORMAL.getCode(), LocalDateTime.now());
            log.info("TCC Cancel - 解冻账户成功, accountId: {}", accountId);
        }

        log.info("TCC Cancel - 冻结账户成功, txId: {}", txId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean prepareUnfreeze(BusinessActionContext actionContext, String accountId,
                                   Integer freezeType, String remark, String operator) {
        String txId = actionContext.getXid();
        log.info("TCC Try - 解冻账户, txId: {}, accountId: {}", txId, accountId);

        saveTccLog(txId, "unfreezeAccountTcc", 1, accountId, buildContext(
                "freezeType", freezeType,
                "remark", remark,
                "operator", operator
        ));

        int rows = accountMapper.unfreezeAccount(
                accountId,
                AccountStatusEnum.NORMAL.getCode(),
                LocalDateTime.now()
        );

        if (rows <= 0) {
            log.error("TCC Try - 解冻账户失败, txId: {}, accountId: {}", txId, accountId);
            return false;
        }

        log.info("TCC Try - 解冻账户成功, txId: {}, accountId: {}", txId, accountId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean commitUnfreeze(BusinessActionContext actionContext) {
        String txId = actionContext.getXid();
        log.info("TCC Confirm - 解冻账户, txId: {}", txId);

        AccountTccLog existLog = tccLogMapper.selectByTxIdAndActionAndPhase(
                txId, "unfreezeAccountTcc", 2);
        if (existLog != null) {
            log.warn("TCC Confirm - 已执行过，跳过, txId: {}", txId);
            return true;
        }

        saveTccLog(txId, "unfreezeAccountTcc", 2,
                (String) actionContext.getActionContext("accountId"), null);

        log.info("TCC Confirm - 解冻账户成功, txId: {}", txId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean rollbackUnfreeze(BusinessActionContext actionContext) {
        String txId = actionContext.getXid();
        String accountId = (String) actionContext.getActionContext("accountId");
        Integer freezeType = (Integer) actionContext.getActionContext("freezeType");
        log.info("TCC Cancel - 解冻账户, txId: {}, accountId: {}", txId, accountId);

        AccountTccLog existLog = tccLogMapper.selectByTxIdAndActionAndPhase(
                txId, "unfreezeAccountTcc", 3);
        if (existLog != null) {
            log.warn("TCC Cancel - 已执行过，跳过, txId: {}", txId);
            return true;
        }

        saveTccLog(txId, "unfreezeAccountTcc", 3, accountId, null);

        if (accountId != null && freezeType != null) {
            accountMapper.freezeAccount(
                    accountId,
                    AccountStatusEnum.FROZEN.getCode(),
                    freezeType,
                    "TCC回滚-重新冻结",
                    LocalDateTime.now(),
                    "system",
                    LocalDateTime.now()
            );
            log.info("TCC Cancel - 重新冻结账户成功, accountId: {}", accountId);
        }

        log.info("TCC Cancel - 解冻账户成功, txId: {}", txId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean prepareClose(BusinessActionContext actionContext, String accountId,
                                String remark, String operator) {
        String txId = actionContext.getXid();
        log.info("TCC Try - 销户, txId: {}, accountId: {}", txId, accountId);

        saveTccLog(txId, "closeAccountTcc", 1, accountId, buildContext(
                "remark", remark,
                "operator", operator
        ));

        int rows = accountMapper.closeAccount(
                accountId,
                AccountStatusEnum.CLOSED.getCode(),
                LocalDateTime.now()
        );

        if (rows <= 0) {
            log.error("TCC Try - 销户失败, txId: {}, accountId: {}", txId, accountId);
            return false;
        }

        log.info("TCC Try - 销户成功, txId: {}, accountId: {}", txId, accountId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean commitClose(BusinessActionContext actionContext) {
        String txId = actionContext.getXid();
        log.info("TCC Confirm - 销户, txId: {}", txId);

        AccountTccLog existLog = tccLogMapper.selectByTxIdAndActionAndPhase(
                txId, "closeAccountTcc", 2);
        if (existLog != null) {
            log.warn("TCC Confirm - 已执行过，跳过, txId: {}", txId);
            return true;
        }

        saveTccLog(txId, "closeAccountTcc", 2,
                (String) actionContext.getActionContext("accountId"), null);

        log.info("TCC Confirm - 销户成功, txId: {}", txId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean rollbackClose(BusinessActionContext actionContext) {
        String txId = actionContext.getXid();
        String accountId = (String) actionContext.getActionContext("accountId");
        log.info("TCC Cancel - 销户, txId: {}, accountId: {}", txId, accountId);

        AccountTccLog existLog = tccLogMapper.selectByTxIdAndActionAndPhase(
                txId, "closeAccountTcc", 3);
        if (existLog != null) {
            log.warn("TCC Cancel - 已执行过，跳过, txId: {}", txId);
            return true;
        }

        saveTccLog(txId, "closeAccountTcc", 3, accountId, null);

        if (accountId != null) {
            Account account = accountMapper.selectByAccountId(accountId);
            if (account != null) {
                account.setStatus(AccountStatusEnum.NORMAL.getCode());
                account.setUpdateTime(LocalDateTime.now());
                accountMapper.updateById(account);
                log.info("TCC Cancel - 恢复账户状态成功, accountId: {}", accountId);
            }
        }

        log.info("TCC Cancel - 销户成功, txId: {}", txId);
        return true;
    }

    private void saveTccLog(String txId, String actionName, Integer phase,
                            String accountId, String context) {
        AccountTccLog log = new AccountTccLog();
        log.setId(SnowflakeIdGenerator.nextId());
        log.setTxId(txId);
        log.setActionName(actionName);
        log.setPhase(phase);
        log.setAccountId(accountId);
        log.setContext(context);
        log.setCreateTime(LocalDateTime.now());
        log.setUpdateTime(LocalDateTime.now());
        tccLogMapper.insert(log);
    }

    private String buildContext(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i + 1 < keyValues.length) {
                map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
            }
        }
        return JSON.toJSONString(map);
    }
}
