package com.boxai.user.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.result.PageResult;
import com.boxai.domain.audit.AuditLog;
import com.boxai.domain.audit.AuditLogQuery;
import com.boxai.domain.audit.AuditLogRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogApplicationServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspacePermissionService workspacePermissionService;

    @InjectMocks
    private AuditLogApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void pageScopesToWorkspaceAndJoinsUser() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "OWNER"));
        AuditLog log = auditLog();
        when(auditLogRepository.page(any())).thenReturn(new PageResult<>(List.of(log), 1, 1, 20));
        User user = new User();
        user.setId(3L);
        user.setEmail("ada@example.com");
        user.setNickname("Ada");
        when(userRepository.findByIds(Set.of(3L))).thenReturn(List.of(user));

        var result = service.page("  agent.create  ", " AGENT ", 3L, "2026-01-02 03:04:05", "  ", 1, 20);

        verify(workspacePermissionService).requirePermission(PermissionCodes.AUDIT_READ);
        ArgumentCaptor<AuditLogQuery> captor = ArgumentCaptor.forClass(AuditLogQuery.class);
        verify(auditLogRepository).page(captor.capture());
        assertEquals(7L, captor.getValue().getWorkspaceId());
        assertEquals("agent.create", captor.getValue().getAction());
        assertEquals("AGENT", captor.getValue().getResourceType());
        assertEquals(LocalDateTime.of(2026, 1, 2, 3, 4, 5), captor.getValue().getStartTime());
        assertNull(captor.getValue().getEndTime());
        assertEquals("ada@example.com", result.records().get(0).userEmail());
        assertEquals("Ada", result.records().get(0).userNickname());
        assertEquals("agent.create", result.records().get(0).action());
    }

    private static AuditLog auditLog() {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setWorkspaceId(7L);
        log.setUserId(3L);
        log.setAction("agent.create");
        log.setResourceType("AGENT");
        log.setResourceId("21");
        log.setResourceName("Helper");
        log.setResult("SUCCESS");
        return log;
    }
}
