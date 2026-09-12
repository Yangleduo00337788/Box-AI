package com.boxai.domain.audit;

import com.boxai.common.result.PageResult;

public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    PageResult<AuditLog> page(AuditLogQuery query);
}
