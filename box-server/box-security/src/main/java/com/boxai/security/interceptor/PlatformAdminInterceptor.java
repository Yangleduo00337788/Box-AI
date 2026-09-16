package com.boxai.security.interceptor;

import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.security.context.LoginUser;
import com.boxai.security.context.SecurityContexts;
import com.boxai.security.permission.PlatformAdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PlatformAdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (PlatformAdminAccess.isPublicPath(request.getRequestURI())) {
            return true;
        }
        LoginUser user = SecurityContexts.currentUser();
        if (!UserTypes.PLATFORM_ADMIN.equals(user.userType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无平台管理权限");
        }
        if (!PlatformAdminAccess.allows(user.platformAdminRole(), request.getMethod(), request.getRequestURI())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前平台角色无权访问该接口");
        }
        return true;
    }
}
