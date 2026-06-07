package com.bank.core.account.tcc;

import com.bank.core.api.dto.TransferDTO;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

@LocalTCC
public interface TransferTccAction {

    @TwoPhaseBusinessAction(name = "transferTccAction", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryTransfer(BusinessActionContext context, TransferDTO dto);

    boolean confirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}
