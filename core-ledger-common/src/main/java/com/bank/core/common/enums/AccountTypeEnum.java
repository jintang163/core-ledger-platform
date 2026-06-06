package com.bank.core.common.enums;

import lombok.Getter;

/**
 * 账户类型枚举
 * 定义系统中所有账户的类型，包括个人账户、企业账户和内部清算账户
 */
@Getter
public enum AccountTypeEnum {

    /** 个人账户 - 个人用户开立的账户 */
    PERSONAL(1, "个人账户"),
    /** 企业账户 - 企业用户开立的账户 */
    ENTERPRISE(2, "企业账户"),
    /** 清算账户 - 用于外部渠道资金清算的内部账户 */
    CLEARING(3, "清算账户");

    private final Integer code;
    private final String desc;

    AccountTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取账户类型枚举
     * @param code 账户类型编码
     * @return 账户类型枚举，找不到返回null
     */
    public static AccountTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AccountTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
