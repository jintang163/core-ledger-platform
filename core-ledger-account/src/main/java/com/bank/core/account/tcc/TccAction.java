package com.bank.core.account.tcc;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

@LocalTCC
public interface TccAction {

    @TwoPhaseBusinessAction(name = "tccAction", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryAction(BusinessActionContext context);

    boolean confirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}
