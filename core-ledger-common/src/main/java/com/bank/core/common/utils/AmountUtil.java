package com.bank.core.common.utils;

import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.ResultCodeEnum;
import com.bank.core.common.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AmountUtil {

    private static final int SCALE = 2;
    private static final long FACTOR = 100L;

    public static long yuanToFen(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.setScale(SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(FACTOR))
                .longValue();
    }

    public static BigDecimal fenToYuan(Long amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amount)
                .divide(BigDecimal.valueOf(FACTOR), SCALE, RoundingMode.HALF_UP);
    }

    public static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "金额不能为空");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "金额不能为负数");
        }
        long fen = yuanToFen(amount);
        if (fen > CommonConstants.MAX_BALANCE) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "金额超出最大限制");
        }
    }

    public static String formatAmount(Long amount) {
        return fenToYuan(amount).toString();
    }
}
