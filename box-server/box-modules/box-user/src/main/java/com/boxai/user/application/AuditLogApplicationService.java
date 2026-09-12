package com.boxai.user.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.result.PageResult;
import com.boxai.domain.audit.AuditLog;
import com.boxai.domain.audit.AuditLogQuery;
import com.boxai.domain.audit.AuditLogRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.user.api.AuditLogVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class AuditLogApplicationService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogRepository auditLogRepository;
    private final WorkspacePermissionService workspacePermissionService;

    public AuditLogApplicationService(AuditLogRepository auditLogRepository,
                                      WorkspacePermissionService workspacePermissionService) {
        this.auditLogRepository = auditLogRepository;
        this.workspacePermissionService = workspacePermissionService;
    }

    public PageResult<AuditLogVO> page(String action,
                                       String resourceType,
                                       Long userId,
                                       String startTime,
                                       String endTime,
                                       int page,
                                       int pageSize) {
        workspacePermissionService.requirePermission(PermissionCodes.AUDIT_READ);
        AuditLogQuery query = new AuditLogQuery();
        query.setWorkspaceId(WorkspaceContext.require().workspaceId());
        query.setAction(trimToNull(action));
        query.setResourceType(trimToNull(resourceType));
        query.setUserId(userId);
        query.setStartTime(parseDateTime(startTime));
        query.setEndTime(parseDateTime(endTime));
        query.setPage(page);
        query.setPageSize(pageSize);
        PageResult<AuditLog> result = auditLogRepository.page(query);
        return new PageResult<>(
                result.records().stream().map(this::toVO).toList(),
                result.total(),
                result.page(),
                result.pageSize());
    }

    private AuditLogVO toVO(AuditLog auditLog) {
        return new AuditLogVO(
                auditLog.getId(),
                auditLog.getWorkspaceId(),
                auditLog.getUserId(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getResourceName(),
                auditLog.getResult(),
                auditLog.getIpAddress(),
                auditLog.getTraceId(),
                auditLog.getDetail(),
                auditLog.getCreatedAt());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME);
        } catch (DateTimeParseException ex) {
            return LocalDateTime.parse(value.trim());
        }
    }
}
