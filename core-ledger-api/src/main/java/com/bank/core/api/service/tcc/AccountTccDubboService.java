package com.bank.core.api.service.tcc;

import com.bank.core.api.dto.AccountFreezeDTO;
import com.bank.core.api.dto.TransferDTO;
import com.bank.core.common.result.Result;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * 账户服务TCC接口 - 跨服务Dubbo调用
 * 
 * 服务边界：Account服务
 * 负责：账户资金冻结、解冻、扣款、入账
 */
@LocalTCC
public interface AccountTccDubboService {

    /**
     * TCC - 转账资金冻结
     * Try阶段：冻结付款方余额，创建冻结状态的转账订单
     */
    @TwoPhaseBusinessAction(name = "transferFreezeTcc", commitMethod = "confirmTransfer", rollbackMethod = "cancelTransfer")
    Result<String> tryTransfer(BusinessActionContext context, TransferDTO dto);

    /**
     * TCC - 转账确认
     * Confirm阶段：解冻并扣款付款方，入账收款方，更新订单状态为成功
     */
    Result<Boolean> confirmTransfer(BusinessActionContext context);

    /**
     * TCC - 转账取消
     * Cancel阶段：解冻付款方余额，更新订单状态为失败
     */
    Result<Boolean> cancelTransfer(BusinessActionContext context);

    /**
     * TCC - 账户冻结
     * Try阶段：冻结账户指定金额
     */
    @TwoPhaseBusinessAction(name = "accountFreezeTcc", commitMethod = "confirmFreeze", rollbackMethod = "cancelFreeze")
    Result<String> tryFreeze(BusinessActionContext context, AccountFreezeDTO dto);

    /**
     * TCC - 冻结确认
     * Confirm阶段：正式冻结，更新冻结状态
     */
    Result<Boolean> confirmFreeze(BusinessActionContext context);

    /**
     * TCC - 冻结取消
     * Cancel阶段：解冻资金，恢复账户状态
     */
    Result<Boolean> cancelFreeze(BusinessActionContext context);
}
