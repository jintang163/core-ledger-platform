package com.bank.core.common.utils;

import cn.hutool.core.util.IdUtil;

public class SnowflakeIdGenerator {

    private static final long WORKER_ID = 1L;
    private static final long DATA_CENTER_ID = 1L;

    private static final cn.hutool.core.lang.Snowflake SNOWFLAKE =
            IdUtil.getSnowflake(WORKER_ID, DATA_CENTER_ID);

    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    public static String nextIdStr() {
        return SNOWFLAKE.nextIdStr();
    }

    public static String generateAccountNo() {
        return "622202" + SNOWFLAKE.nextIdStr().substring(6, 18);
    }

    public static String generateRequestId() {
        return "REQ" + System.currentTimeMillis() + SNOWFLAKE.nextIdStr().substring(12);
    }

    public static String generateTraceId() {
        return "TRACE" + System.currentTimeMillis() + SNOWFLAKE.nextIdStr().substring(12);
    }

    public static String generateTransactionNo() {
        return "TXN" + System.currentTimeMillis() + SNOWFLAKE.nextIdStr().substring(10);
    }

    public static String generateVoucherNo() {
        return "VCH" + System.currentTimeMillis() + SNOWFLAKE.nextIdStr().substring(10);
    }

    public static String generateEntryId() {
        return "ENTRY" + SNOWFLAKE.nextIdStr();
    }
}
