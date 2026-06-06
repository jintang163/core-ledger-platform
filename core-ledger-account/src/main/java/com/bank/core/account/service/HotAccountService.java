package com.bank.core.account.service;

import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.AccountShard;

import java.math.BigDecimal;
import java.util.List;

/**
 * 热点账户服务接口
 * 提供热点账户分片、路由、归并等功能
 */
public interface HotAccountService {

    /**
     * 判断账户是否为热点账户
     * @param accountId 账户ID
     * @return 是否为热点账户
     */
    boolean isHotAccount(String accountId);

    /**
     * 创建热点账户分片
     * @param mainAccountId 主账户ID
     * @param shardCount 分片数量
     * @param shardingStrategy 分片策略
     * @return 分片列表
     */
    List<AccountShard> createShards(String mainAccountId, Integer shardCount, Integer shardingStrategy);

    /**
     * 获取路由的分片（根据策略选择合适的影子账户）
     * @param mainAccountId 主账户ID
     * @return 选中的分片
     */
    AccountShard routeShard(String mainAccountId);

    /**
     * 更新影子账户余额
     * @param shardId 分片ID
     * @param amountFen 变动金额（分），正数增加，负数减少
     * @return 是否更新成功
     */
    boolean updateShardBalance(String shardId, Long amountFen);

    /**
     * 归并影子账户余额到主账户
     * @param mainAccountId 主账户ID
     * @return 归并的总金额（分）
     */
    Long mergeShards(String mainAccountId);

    /**
     * 定时归并所有热点账户的影子分片
     */
    void mergeAllShards();

    /**
     * 获取主账户的可用余额（主账户余额 + 所有影子账户余额）
     * @param mainAccountId 主账户ID
     * @return 总可用余额（分）
     */
    Long getTotalAvailableBalance(String mainAccountId);

    /**
     * 查询主账户的所有影子分片
     * @param mainAccountId 主账户ID
     * @return 分片列表
     */
    List<AccountShard> getShards(String mainAccountId);

    /**
     * 标记账户为热点账户
     * @param accountId 账户ID
     * @param shardCount 分片数量
     */
    void markAsHotAccount(String accountId, Integer shardCount);

    /**
     * 取消热点账户标记
     * @param accountId 账户ID
     */
    void unmarkAsHotAccount(String accountId);
}
