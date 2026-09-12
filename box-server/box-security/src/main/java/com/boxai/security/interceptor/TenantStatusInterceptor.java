package com.boxai.security.interceptor;

import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.security.context.LoginUser;
import com.boxai.security.context.SecurityContexts;
import com.boxai.security.tenant.TenantAccessGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantStatusInterceptor implements HandlerInterceptor {

    private final TenantAccessGuard tenantAccessGuard;

    public TenantStatusInterceptor(TenantAccessGuard tenantAccessGuard) {
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublicPath(request.getRequestURI())) {
            return true;
        }
        LoginUser user = currentUserOrNull();
        if (user == null || UserTypes.PLATFORM_ADMIN.equals(user.userType())) {
            return true;
        }
        tenantAccessGuard.ensurePrimaryTenantActive(user.userId());
        return true;
    }

    private boolean isPublicPath(String uri) {
        return uri.startsWith("/api/v1/auth/login")
                || uri.startsWith("/api/v1/auth/register")
                || uri.startsWith("/api/v1/auth/verification-code")
                || uri.startsWith("/api/v1/auth/password/reset")
                || uri.startsWith("/api/v1/published/")
                || uri.startsWith("/api/v1/hooks/")
                || uri.equals("/api/v1/system/health")
                || uri.equals("/api/v1/system/content");
    }

    private LoginUser currentUserOrNull() {
        try {
            return SecurityContexts.currentUser();
        } catch (BusinessException e) {
            return null;
        }
    }
}
