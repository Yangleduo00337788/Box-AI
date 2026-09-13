package com.boxai.common.security;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmbedDomainNormalizer {

    private static final Pattern HOST = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$");

    private EmbedDomainNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        value = value.replaceFirst("^https?://", "");
        int slash = value.indexOf('/');
        if (slash >= 0) {
            value = value.substring(0, slash);
        }
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(0, colon);
        }
        if (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty()) {
            return "";
        }
        if ("localhost".equals(value) || HOST.matcher(value).matches()) {
            return value;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "自定义域名格式无效");
    }
}
