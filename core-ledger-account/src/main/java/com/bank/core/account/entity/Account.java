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

@Data
@TableName("t_account")
@ApiModel(value = "账户信息", description = "账户基本信息")
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String accountId;

    private String accountNo;

    private String userId;

    private Integer accountType;

    private String currency;

    private Long balance;

    private Integer status;

    private Integer freezeType;

    private String freezeRemark;

    private LocalDateTime freezeTime;

    private String freezeOperator;

    private LocalDateTime openTime;

    private LocalDateTime closeTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Version
    private Integer version;

    /** 热点账户状态（0-普通账户, 1-待标记热点, 2-已分片热点, 3-已启用缓冲记账） */
    @ApiModelProperty(value = "热点账户状态", example = "0",
            notes = "0-普通账户, 1-待标记热点, 2-已分片热点, 3-已启用缓冲记账")
    private Integer hotStatus;

    /** 影子账户分片数量 */
    @ApiModelProperty(value = "影子账户分片数量", example = "10",
            notes = "热点账户的影子账户分片数量")
    private Integer shardCount;

    /** 分片策略（1-随机路由, 2-轮询路由, 3-哈希路由） */
    @ApiModelProperty(value = "分片策略", example = "1",
            notes = "1-随机路由, 2-轮询路由, 3-哈希路由")
    private Integer shardingStrategy;

    @TableLogic
    @ApiModelProperty(hidden = true)
    private Integer deleted;
}
