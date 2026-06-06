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
 * 账户影子分片实体
 *
 * 核心功能：
 * 1. 热点账户分片：将热点账户拆分为多个影子账户，分散并发压力
 * 2. 资金隔离：每个分片独立存储余额，避免主账户并发冲突
 * 3. 定时归并：影子账户余额定期（默认凌晨2点）归并到主账户
 *
 * 业务流程：
 * 资金流向：扣款时路由到影子账户，低峰期归并到主账户
 * 资金路径：主账户 <-> 影子账户（分片） <-> 外部交易
 *
 * 设计要点：
 * - 分片ID规则：主账户ID + 后缀 + 两位序号（如 ACC1001_SHARD_01）
 * - 分片索引从0开始，连续编号
 * - 使用乐观锁（version）防止并发更新
 * - 归并状态用于追踪分片归并进度
 */
@Data
@TableName("t_account_shard")
@ApiModel(value = "账户影子分片", description = "热点账户的影子分片信息")
public class AccountShard implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    @ApiModelProperty(value = "主键ID", hidden = true)
    private Long id;

    /** 分片ID（账户ID + 分片索引） */
    @ApiModelProperty(value = "分片ID", example = "ACC1000001_SHARD_01",
            notes = "账户ID + 分片索引，唯一标识一个影子账户")
    private String shardId;

    /** 主账户ID（关联的热点账户ID） */
    @ApiModelProperty(value = "主账户ID", example = "ACC1000001",
            notes = "关联的热点主账户ID")
    private String mainAccountId;

    /** 分片索引（从0开始） */
    @ApiModelProperty(value = "分片索引", example = "1",
            notes = "分片编号，从0开始")
    private Integer shardIndex;

    /** 分片账户号 */
    @ApiModelProperty(value = "分片账户号", example = "6222021234567890123",
            notes = "影子账户的账号")
    private String shardAccountNo;

    /** 币种 */
    @ApiModelProperty(value = "币种", example = "CNY")
    private String currency;

    /** 余额（单位：分） */
    @ApiModelProperty(value = "余额", example = "100000",
            notes = "单位：分")
    private Long balance;

    /** 分片状态（0-正常, 1-已关闭） */
    @ApiModelProperty(value = "分片状态", example = "0",
            notes = "0-正常, 1-已关闭")
    private Integer status;

    /** 最后归并时间 */
    @ApiModelProperty(value = "最后归并时间")
    private LocalDateTime lastMergeTime;

    /** 归并状态（0-未归并, 1-归并中, 2-已归并） */
    @ApiModelProperty(value = "归并状态", example = "0",
            notes = "0-未归并, 1-归并中, 2-已归并")
    private Integer mergeStatus;

    @Version
    @ApiModelProperty(hidden = true)
    private Integer version;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @ApiModelProperty(hidden = true)
    private Integer deleted;
}
