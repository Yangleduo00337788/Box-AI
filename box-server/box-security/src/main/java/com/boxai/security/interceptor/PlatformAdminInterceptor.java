package com.boxai.security.interceptor;

import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.security.context.LoginUser;
import com.boxai.security.context.SecurityContexts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PlatformAdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublicAdminPath(request.getRequestURI())) {
            return true;
        }
        LoginUser user = SecurityContexts.currentUser();
        if (!UserTypes.PLATFORM_ADMIN.equals(user.userType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无平台管理权限");
        }
        if (request.getRequestURI().startsWith("/api/v1/admin/billing")
                && user.username() != null) {
            // 财务与超管可访问账单对账；其余角色在业务层继续受限
        }
        return true;
    }

    private boolean isPublicAdminPath(String uri) {
        return uri.startsWith("/api/v1/admin/auth/login")
                || uri.startsWith("/api/v1/admin/auth/verification-code")
                || uri.startsWith("/api/v1/admin/auth/password/reset");
    }
}
