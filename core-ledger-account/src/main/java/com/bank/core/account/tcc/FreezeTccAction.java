package com.bank.core.account.tcc;

import com.bank.core.api.dto.AccountFreezeDTO;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * 冻结TCC接口
 * 用于账户冻结的TCC事务控制
 * 
 * 注意：
 * - Try方法通过Seata代理Bean调用，不能手动new BusinessActionContext()
 * - Confirm/Cancel从Seata持久化上下文恢复参数
 * - 参数通过@BusinessActionContextParameter传递到二阶段
 */
@LocalTCC
public interface FreezeTccAction {

    /**
     * TCC Try阶段 - 账户资金冻结
     * 冻结账户指定金额
     * 
     * @param accountId 账户ID
     * @param amount 冻结金额
     * @param dto 冻结请求DTO
     * @return 是否成功
     */
    @TwoPhaseBusinessAction(name = "freezeTccAction", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryFreeze(
            BusinessActionContext context,
            @BusinessActionContextParameter(paramName = "accountId") String accountId,
            @BusinessActionContextParameter(paramName = "amount") java.math.BigDecimal amount,
            AccountFreezeDTO dto);

    /**
     * TCC Confirm阶段 - 冻结确认
     * 正式冻结，更新冻结状态
     * 
     * @param context Seata事务上下文
     * @return 是否成功
     */
    boolean confirm(BusinessActionContext context);

    /**
     * TCC Cancel阶段 - 冻结取消
     * 解冻资金，恢复账户状态
     * 
     * @param context Seata事务上下文
     * @return 是否成功
     */
    boolean cancel(BusinessActionContext context);
}
