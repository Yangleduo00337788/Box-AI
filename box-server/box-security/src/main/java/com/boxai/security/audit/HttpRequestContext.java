package com.boxai.security.audit;

import com.boxai.common.constant.HeaderNames;
import com.boxai.common.constant.RequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class HttpRequestContext {

    private HttpRequestContext() {
    }

    public static String clientIp() {
        org.springframework.web.context.request.RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    public static String requestId() {
        org.springframework.web.context.request.RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        Object value = request.getAttribute(RequestAttributes.REQUEST_ID);
        if (value instanceof String requestId && !requestId.isBlank()) {
            return requestId;
        }
        String header = request.getHeader(HeaderNames.REQUEST_ID);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return null;
    }
}
