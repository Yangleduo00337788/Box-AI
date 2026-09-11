package com.boxai.security.context;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;

public record WorkspaceContext(Long workspaceId, Long userId, Long roleId, String roleCode) {

    private static final ThreadLocal<WorkspaceContext> HOLDER = new ThreadLocal<>();

    public static void set(WorkspaceContext context) {
        HOLDER.set(context);
    }

    public static WorkspaceContext get() {
        return HOLDER.get();
    }

    public static WorkspaceContext require() {
        WorkspaceContext context = get();
        if (context == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少 X-Workspace-Id");
        }
        return context;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
