package com.boxai.common.security;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;

import java.security.SecureRandom;
import java.util.Locale;

public final class EnterpriseAccounts {

    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private EnterpriseAccounts() {
    }

    public static String normalizeLoginName(String account) {
        if (account == null || account.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写登录账号");
        }
        String value = account.trim().toLowerCase(Locale.ROOT);
        if (value.length() < 2 || value.length() > 64 || !value.matches("[a-z0-9._@+-]+")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "账号为 2-64 位字母、数字、点、下划线、短横线或邮箱");
        }
        return value;
    }

    public static String normalizeOrgId(String orgId) {
        if (orgId == null || orgId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写企业标识");
        }
        return orgId.trim().toLowerCase(Locale.ROOT);
    }

    public static String internalUsername(Long tenantId, String loginName) {
        return "t" + tenantId + "_" + loginName;
    }

    public static String randomInviteCode() {
        char[] buffer = new char[10];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)];
        }
        return new String(buffer);
    }
}
