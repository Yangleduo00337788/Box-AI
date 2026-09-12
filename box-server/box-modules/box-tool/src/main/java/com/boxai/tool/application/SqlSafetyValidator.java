package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;

import java.util.regex.Pattern;

public final class SqlSafetyValidator {

    private static final Pattern BLOCKED_KEYWORDS = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|CREATE|GRANT|REVOKE|EXEC|EXECUTE|CALL|MERGE|REPLACE)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MULTI_STATEMENT = Pattern.compile(";\\s*\\S");

    private SqlSafetyValidator() {
    }

    public static String validateSelectOnly(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SQL 不能为空");
        }
        String trimmed = sql.trim();
        if (MULTI_STATEMENT.matcher(trimmed).find()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅允许单条 SQL 语句");
        }
        if (!trimmed.regionMatches(true, 0, "SELECT", 0, 6)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "V1 仅允许 SELECT 查询");
        }
        if (BLOCKED_KEYWORDS.matcher(trimmed).find()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SQL 包含不允许的操作");
        }
        return trimmed;
    }
}
