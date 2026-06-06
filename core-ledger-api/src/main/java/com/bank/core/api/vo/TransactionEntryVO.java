package com.bank.core.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易分录返回VO
 *
 * 核心功能：
 * 1. 封装交易分录的返回数据
 * 2. 提供给前端/调用方展示使用
 * 3. 包含借贷方向的文字描述，便于展示
 *
 * 设计要点：
 * - VO（View Object）专门用于返回视图层数据
 * - 包含directionDesc字段，将数字类型的direction转换为文字描述
 * - 不包含敏感字段和内部字段
 * - 金额单位为元，便于前端展示
 */
@Data
public class TransactionEntryVO implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 分录ID
     * 业务主键，唯一标识一条分录
     */
    private String entryId;

    /**
     * 关联的交易ID
     * 用于关联主交易记录
     */
    private String transactionId;

    /**
     * 账户ID
     * 本条分录对应的账户
     */
    private String accountId;

    /**
     * 账户号
     * 便于展示给用户
     */
    private String accountNo;

    /**
     * 会计科目代码
     * 如：1001-库存现金
     */
    private String subjectCode;

    /**
     * 会计科目名称
     * 与科目代码对应
     */
    private String subjectName;

    /**
     * 借贷方向（数字编码）
     * 1-借方, 2-贷方
     */
    private Integer direction;

    /**
     * 借贷方向描述（文字）
     * 如："借方"、"贷方"
     * 由服务层根据direction字段转换生成，便于前端展示
     */
    private String directionDesc;

    /**
     * 分录金额（单位：元）
     */
    private BigDecimal amount;

    /**
     * 币种
     * CNY-人民币, USD-美元等
     */
    private String currency;

    /**
     * 分录摘要
     */
    private String summary;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
