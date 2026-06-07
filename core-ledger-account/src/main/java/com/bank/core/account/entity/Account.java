package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 账户实体类
 *
 * 核心功能：
 * 1. 存储账户的基本信息、余额、状态等核心数据
 * 2. 支持热点账户标记和分片配置
 * 3. 使用乐观锁（version）防止并发更新冲突
 * 4. 使用逻辑删除（deleted）实现软删除
 *
 * 设计要点：
 * - 余额单位为分，使用Long类型避免浮点精度问题
 * - 支持多种账户类型（个人账户、企业账户等）
 * - 支持多种状态（正常、冻结、已销户等）
 * - 热点账户状态用于标识是否启用分片或缓冲记账
 *
 * 分片策略：
 * - 分库键：account_id 哈希分片（确保同一账户数据在同一库）
 * - 不分表（单表单库存储账户信息）
 * - 索引优化：(account_id) 唯一索引，(user_id, account_type, currency) 联合索引
 */
@Data
@TableName("t_account")
@ApiModel(value = "账户信息", description = "账户基本信息")
public class Account implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /** 主键ID（雪花算法生成） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 账户ID（业务主键，系统唯一标识） */
    private String accountId;

    /** 账户号（展示给用户的账号） */
    private String accountNo;

    /** 用户ID（关联的用户） */
    private String userId;

    /** 账户类型（1-个人账户, 2-企业账户, 3-内部账户等） */
    private Integer accountType;

    /** 币种（CNY-人民币, USD-美元等） */
    private String currency;

    /** 账户余额（单位：分） */
    private Long balance;

    /** 冻结余额（单位：分），TCC模式中Try阶段预留的资金 */
    private Long freezeBalance;

    /** 账户状态（0-正常, 1-冻结, 2-已销户） */
    private Integer status;

    /** 冻结类型（1-司法冻结, 2-业务冻结等） */
    private Integer freezeType;

    /** 冻结备注 */
    private String freezeRemark;

    /** 冻结时间 */
    private LocalDateTime freezeTime;

    /** 冻结操作员 */
    private String freezeOperator;

    /** 开户时间 */
    private LocalDateTime openTime;

    /** 销户时间 */
    private LocalDateTime closeTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 乐观锁版本号（用于并发更新控制） */
    @Version
    private Integer version;

    /**
     * 热点账户状态
     * 0-普通账户, 1-待标记热点, 2-已分片热点, 3-已启用缓冲记账
     */
    @ApiModelProperty(value = "热点账户状态", example = "0",
            notes = "0-普通账户, 1-待标记热点, 2-已分片热点, 3-已启用缓冲记账")
    private Integer hotStatus;

    /** 影子账户分片数量（热点账户专用） */
    @ApiModelProperty(value = "影子账户分片数量", example = "10",
            notes = "热点账户的影子账户分片数量")
    private Integer shardCount;

    /**
     * 分片策略
     * 1-随机路由, 2-轮询路由, 3-哈希路由
     */
    @ApiModelProperty(value = "分片策略", example = "1",
            notes = "1-随机路由, 2-轮询路由, 3-哈希路由")
    private Integer shardingStrategy;

    /** 逻辑删除标记（0-未删除, 1-已删除） */
    @TableLogic
    @ApiModelProperty(hidden = true)
    private Integer deleted;
}
