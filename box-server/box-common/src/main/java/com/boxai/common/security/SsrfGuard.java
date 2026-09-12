package com.boxai.common.security;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

public final class SsrfGuard {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private SsrfGuard() {
    }

    public static URI validateHttpUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 不能为空");
        }
        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 格式无效");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅允许 http/https 协议");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 缺少主机名");
        }
        if (isBlockedHost(host)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "禁止访问内网或本地地址");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isBlockedAddress(address)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "禁止访问内网或本地地址");
                }
            }
        } catch (UnknownHostException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法解析 URL 主机名");
        }
        return uri;
    }

    private static boolean isBlockedHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.equals("localhost")
                || normalized.endsWith(".localhost")
                || normalized.endsWith(".local")
                || normalized.equals("0.0.0.0")
                || normalized.equals("metadata.google.internal")) {
            return true;
        }
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return isBlockedLiteralIp(normalized);
    }

    private static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            if (first == 10) {
                return true;
            }
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            if (first == 192 && second == 168) {
                return true;
            }
            if (first == 127) {
                return true;
            }
            if (first == 0) {
                return true;
            }
            if (first == 169 && second == 254) {
                return true;
            }
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }
        }
        if (bytes.length == 16) {
            int first = bytes[0] & 0xFF;
            if (first == 0xFC || first == 0xFD) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlockedLiteralIp(String host) {
        if (!host.chars().allMatch(ch -> Character.isDigit(ch) || ch == '.')) {
            return false;
        }
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            if (first == 10 || first == 127 || first == 0) {
                return true;
            }
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            if (first == 192 && second == 168) {
                return true;
            }
            if (first == 169 && second == 254) {
                return true;
            }
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }
        } catch (NumberFormatException ignored) {
            return false;
        }
        return false;
    }
}
