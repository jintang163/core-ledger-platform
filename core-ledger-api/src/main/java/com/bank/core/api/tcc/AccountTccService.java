package com.bank.core.api.tcc;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.math.BigDecimal;

@LocalTCC
public interface AccountTccService {

    @TwoPhaseBusinessAction(name = "createAccountTcc", commitMethod = "commit", rollbackMethod = "rollback")
    boolean prepareCreate(
            BusinessActionContext actionContext,
            @BusinessActionContextParameter(paramName = "accountId") String accountId,
            @BusinessActionContextParameter(paramName = "userId") String userId,
            @BusinessActionContextParameter(paramName = "accountType") Integer accountType,
            @BusinessActionContextParameter(paramName = "currency") String currency,
            @BusinessActionContextParameter(paramName = "initBalance") BigDecimal initBalance
    );

    boolean commit(BusinessActionContext actionContext);

    boolean rollback(BusinessActionContext actionContext);

    @TwoPhaseBusinessAction(name = "freezeAccountTcc", commitMethod = "commitFreeze", rollbackMethod = "rollbackFreeze")
    boolean prepareFreeze(
            BusinessActionContext actionContext,
            @BusinessActionContextParameter(paramName = "accountId") String accountId,
            @BusinessActionContextParameter(paramName = "freezeType") Integer freezeType,
            @BusinessActionContextParameter(paramName = "remark") String remark,
            @BusinessActionContextParameter(paramName = "operator") String operator
    );

    boolean commitFreeze(BusinessActionContext actionContext);

    boolean rollbackFreeze(BusinessActionContext actionContext);

    @TwoPhaseBusinessAction(name = "unfreezeAccountTcc", commitMethod = "commitUnfreeze", rollbackMethod = "rollbackUnfreeze")
    boolean prepareUnfreeze(
            BusinessActionContext actionContext,
            @BusinessActionContextParameter(paramName = "accountId") String accountId,
            @BusinessActionContextParameter(paramName = "freezeType") Integer freezeType,
            @BusinessActionContextParameter(paramName = "remark") String remark,
            @BusinessActionContextParameter(paramName = "operator") String operator
    );

    boolean commitUnfreeze(BusinessActionContext actionContext);

    boolean rollbackUnfreeze(BusinessActionContext actionContext);

    @TwoPhaseBusinessAction(name = "closeAccountTcc", commitMethod = "commitClose", rollbackMethod = "rollbackClose")
    boolean prepareClose(
            BusinessActionContext actionContext,
            @BusinessActionContextParameter(paramName = "accountId") String accountId,
            @BusinessActionContextParameter(paramName = "remark") String remark,
            @BusinessActionContextParameter(paramName = "operator") String operator
    );

    boolean commitClose(BusinessActionContext actionContext);

    boolean rollbackClose(BusinessActionContext actionContext);
}
