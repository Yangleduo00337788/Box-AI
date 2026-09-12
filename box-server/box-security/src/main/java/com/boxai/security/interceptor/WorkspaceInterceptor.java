package com.boxai.security.interceptor;

import com.boxai.common.constant.HeaderNames;
import com.boxai.common.constant.RequestAttributes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.security.context.SecurityContexts;
import com.boxai.security.context.WorkspaceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class WorkspaceInterceptor implements HandlerInterceptor {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceInterceptor(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublishedApiPath(request)) {
            Object workspaceId = request.getAttribute(RequestAttributes.API_KEY_WORKSPACE_ID);
            Object userId = request.getAttribute(RequestAttributes.API_KEY_USER_ID);
            if (workspaceId instanceof Long wsId && userId instanceof Long uid) {
                WorkspaceContext.set(new WorkspaceContext(wsId, uid, null, "API_KEY"));
            }
            return true;
        }
        if (isWebhookPath(request)) {
            return true;
        }
        if (isPublicPath(request.getRequestURI())) {
            return true;
        }
        String workspaceIdHeader = request.getHeader(HeaderNames.WORKSPACE_ID);
        if (workspaceIdHeader == null || workspaceIdHeader.isBlank()) {
            return true;
        }
        LoginUser user = currentUserOrNull();
        if (user == null) {
            return true;
        }
        Long workspaceId;
        try {
            workspaceId = Long.valueOf(workspaceIdHeader);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作空间 ID 无效");
        }
        var member = workspaceRepository.findMember(workspaceId, user.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该工作空间"));
        if (member.getStatus() == null || member.getStatus() != 1) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "工作空间成员已禁用");
        }
        WorkspaceContext.set(new WorkspaceContext(workspaceId, user.userId(), member.getRoleId(), member.getRoleCode()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        WorkspaceContext.clear();
    }

    private boolean isPublishedApiPath(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/v1/published/");
    }

    private boolean isWebhookPath(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/v1/hooks/");
    }

    private boolean isPublicPath(String uri) {
        return uri.startsWith("/api/v1/auth/")
                || uri.startsWith("/api/v1/admin/")
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
