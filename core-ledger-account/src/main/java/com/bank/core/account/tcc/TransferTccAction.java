package com.bank.core.account.tcc;

import com.bank.core.api.dto.TransferDTO;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * 转账TCC接口
 * 用于行内转账的TCC事务控制
 * 
 * 注意：
 * - Try方法通过Seata代理Bean调用，不能手动new BusinessActionContext()
 * - Confirm/Cancel从Seata持久化上下文恢复参数
 * - 参数通过@BusinessActionContextParameter传递到二阶段
 */
@LocalTCC
public interface TransferTccAction {

    /**
     * TCC Try阶段 - 转账资金冻结
     * 冻结付款方余额，创建冻结状态的转账订单
     * 
     * @param businessNo 业务流水号（通过@BusinessActionContextParameter传递到二阶段）
     * @param fromAccountId 付款方账户ID
     * @param toAccountId 收款方账户ID
     * @param amount 转账金额（元）
     * @param currency 币种
     * @param dto 完整的转账请求DTO
     * @return 是否成功
     */
    @TwoPhaseBusinessAction(name = "transferTccAction", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryTransfer(
            BusinessActionContext context,
            @BusinessActionContextParameter(paramName = "businessNo") String businessNo,
            @BusinessActionContextParameter(paramName = "fromAccountId") String fromAccountId,
            @BusinessActionContextParameter(paramName = "toAccountId") String toAccountId,
            @BusinessActionContextParameter(paramName = "amount") java.math.BigDecimal amount,
            @BusinessActionContextParameter(paramName = "currency") String currency,
            TransferDTO dto);

    /**
     * TCC Confirm阶段 - 转账确认
     * 解冻并扣款付款方，入账收款方，更新订单状态为成功
     * 
     * @param context Seata事务上下文（Seata自动注入）
     * @return 是否成功
     */
    boolean confirm(BusinessActionContext context);

    /**
     * TCC Cancel阶段 - 转账取消
     * 解冻付款方余额，更新订单状态为失败
     * 
     * @param context Seata事务上下文（Seata自动注入）
     * @return 是否成功
     */
    boolean cancel(BusinessActionContext context);
}
