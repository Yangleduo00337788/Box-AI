package com.boxai.infrastructure.persistence.repository;

import com.boxai.common.result.PageResult;
import com.boxai.domain.audit.AuditLog;
import com.boxai.domain.audit.AuditLogQuery;
import com.boxai.domain.audit.AuditLogRepository;
import com.boxai.infrastructure.persistence.entity.AuditLogDO;
import com.boxai.infrastructure.persistence.mapper.AuditLogMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogMapper mapper;

    public AuditLogRepositoryImpl(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogDO row = toDo(auditLog);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        auditLog.setId(row.getId());
        auditLog.setCreatedAt(row.getCreatedAt());
        return auditLog;
    }

    @Override
    public PageResult<AuditLog> page(AuditLogQuery query) {
        int page = Math.max(query.getPage(), 1);
        int pageSize = Math.min(Math.max(query.getPageSize(), 1), 100);
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("workspace_id", query.getWorkspaceId(), query.getWorkspaceId() != null)
                .eq("action", query.getAction(), query.getAction() != null && !query.getAction().isBlank())
                .eq("resource_type", query.getResourceType(), query.getResourceType() != null && !query.getResourceType().isBlank())
                .eq("user_id", query.getUserId(), query.getUserId() != null)
                .ge("created_at", query.getStartTime(), query.getStartTime() != null)
                .le("created_at", query.getEndTime(), query.getEndTime() != null)
                .orderBy("created_at", false);
        Page<AuditLogDO> result = mapper.paginate(page, pageSize, wrapper);
        List<AuditLog> records = result.getRecords().stream().map(this::toDomain).toList();
        return new PageResult<>(records, result.getTotalRow(), page, pageSize);
    }

    private AuditLog toDomain(AuditLogDO row) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(row.getId());
        auditLog.setWorkspaceId(row.getWorkspaceId());
        auditLog.setUserId(row.getUserId());
        auditLog.setAction(row.getAction());
        auditLog.setResourceType(row.getResourceType());
        auditLog.setResourceId(row.getResourceId());
        auditLog.setResourceName(row.getResourceName());
        auditLog.setResult(row.getResult());
        auditLog.setIpAddress(row.getIpAddress());
        auditLog.setTraceId(row.getTraceId());
        auditLog.setDetail(row.getDetail());
        auditLog.setCreatedAt(row.getCreatedAt());
        return auditLog;
    }

    private AuditLogDO toDo(AuditLog auditLog) {
        AuditLogDO row = new AuditLogDO();
        row.setWorkspaceId(auditLog.getWorkspaceId());
        row.setUserId(auditLog.getUserId());
        row.setAction(auditLog.getAction());
        row.setResourceType(auditLog.getResourceType());
        row.setResourceId(auditLog.getResourceId());
        row.setResourceName(auditLog.getResourceName());
        row.setResult(auditLog.getResult());
        row.setIpAddress(auditLog.getIpAddress());
        row.setTraceId(auditLog.getTraceId());
        row.setDetail(auditLog.getDetail());
        return row;
    }
}
