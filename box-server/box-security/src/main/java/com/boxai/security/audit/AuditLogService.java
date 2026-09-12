package com.boxai.security.audit;

import com.boxai.domain.audit.AuditLog;
import com.boxai.domain.audit.AuditLogRepository;
import com.boxai.security.context.WorkspaceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILED = "FAILED";

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void recordSuccess(String action,
                              String resourceType,
                              String resourceId,
                              String resourceName,
                              String detail) {
        record(action, resourceType, resourceId, resourceName, RESULT_SUCCESS, detail, null, null);
    }

    public void recordSuccess(String action,
                              String resourceType,
                              Long resourceId,
                              String resourceName,
                              String detail) {
        record(action, resourceType, resourceId == null ? null : String.valueOf(resourceId),
                resourceName, RESULT_SUCCESS, detail, null, null);
    }

    public void recordForUser(Long userId,
                              Long workspaceId,
                              String action,
                              String resourceType,
                              String resourceId,
                              String resourceName,
                              String result,
                              String detail) {
        record(action, resourceType, resourceId, resourceName, result, detail, userId, workspaceId);
    }

    private void record(String action,
                        String resourceType,
                        String resourceId,
                        String resourceName,
                        String result,
                        String detail,
                        Long explicitUserId,
                        Long explicitWorkspaceId) {
        try {
            Long userId = explicitUserId;
            Long workspaceId = explicitWorkspaceId;
            if (userId == null || workspaceId == null) {
                WorkspaceContext context = WorkspaceContext.get();
                if (context != null) {
                    if (userId == null) {
                        userId = context.userId();
                    }
                    if (workspaceId == null) {
                        workspaceId = context.workspaceId();
                    }
                }
            }
            AuditLog auditLog = new AuditLog();
            auditLog.setWorkspaceId(workspaceId);
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            auditLog.setResourceName(resourceName);
            auditLog.setResult(result == null ? RESULT_SUCCESS : result);
            auditLog.setIpAddress(HttpRequestContext.clientIp());
            auditLog.setTraceId(HttpRequestContext.requestId());
            auditLog.setDetail(detail);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Audit log write skipped, action={}", action, e);
        }
    }
}
