package com.bank.core.account.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.api.dto.TransactionCreateDTO;
import com.bank.core.api.dto.TransactionQueryDTO;
import com.bank.core.api.vo.TransactionVO;

/**
 * 交易服务接口
 *
 * 核心功能：
 * 1. 复式记账：创建包含借贷分录的交易记录
 * 2. 交易查询：支持多维度查询交易明细
 * 3. 幂等处理：保证同一请求不会重复记账
 *
 * 设计要点：
 * - 每笔交易必须借贷平衡（借方合计 = 贷方合计）
 * - 使用分布式锁防止同一账户并发更新
 * - 热点账户自动路由到影子账户或使用缓冲记账
 * - 支持乐观锁重试机制处理并发冲突
 */
public interface TransactionService {

    /**
     * 创建交易（复式记账）
     *
     * 业务流程：
     * 1. 校验参数合法性和借贷平衡性
     * 2. 幂等性校验（通过requestId和businessNo）
     * 3. 获取所有涉及账户的分布式锁（按ID排序防止死锁）
     * 4. 检查账户状态和余额（扣款账户需检查余额是否充足）
     * 5. 判断账户是否为热点账户，决定是否使用分片或缓冲
     * 6. 逐笔更新账户余额（使用乐观锁+重试机制）
     * 7. 记录交易和分录明细
     * 8. 发送交易成功通知
     *
     * @param dto 交易创建请求参数
     * @return 交易创建结果，包含交易ID和状态
     * @throws BusinessException 参数校验失败、余额不足、并发冲突等业务异常
     */
    TransactionVO createTransaction(TransactionCreateDTO dto);

    /**
     * 根据交易ID查询交易详情
     *
     * @param transactionId 交易ID
     * @return 交易详情（包含所有分录信息）
     * @throws BusinessException 交易不存在时抛出
     */
    TransactionVO getTransaction(String transactionId);

    /**
     * 根据业务流水号查询交易
     *
     * @param businessNo 业务流水号（调用方传入）
     * @return 交易详情
     * @throws BusinessException 交易不存在时抛出
     */
    TransactionVO getTransactionByBusinessNo(String businessNo);

    /**
     * 分页查询交易列表
     *
     * 支持的查询维度：
     * - 账户ID：查询该账户相关的所有交易
     * - 交易类型：按交易类型过滤
     * - 交易状态：按状态过滤
     * - 时间范围：按交易时间范围过滤
     *
     * @param dto 查询条件（包含分页参数）
     * @return 分页的交易列表
     */
    Page<TransactionVO> queryTransactions(TransactionQueryDTO dto);
}
