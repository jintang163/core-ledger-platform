package com.bank.core.account.tcc;

import com.bank.core.api.dto.AccountFreezeDTO;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

@LocalTCC
public interface FreezeTccAction {

    @TwoPhaseBusinessAction(name = "freezeTccAction", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryFreeze(BusinessActionContext context, AccountFreezeDTO dto);

    boolean confirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}
