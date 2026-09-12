package com.boxai.security.audit;

import com.boxai.domain.audit.AuditLog;
import com.boxai.domain.audit.AuditLogRepository;
import com.boxai.security.context.WorkspaceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void recordSuccessUsesWorkspaceContext() {
        WorkspaceContext.set(new WorkspaceContext(100L, 200L, 1L, "TENANT_ADMIN"));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditLogService.recordSuccess("agent.create", "agent", 1L, "Demo Agent", null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(100L, saved.getWorkspaceId());
        assertEquals(200L, saved.getUserId());
        assertEquals("agent.create", saved.getAction());
        assertEquals("agent", saved.getResourceType());
        assertEquals("1", saved.getResourceId());
        assertEquals("Demo Agent", saved.getResourceName());
        assertEquals(AuditLogService.RESULT_SUCCESS, saved.getResult());
    }

    @Test
    void recordForUserAllowsExplicitIdentity() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditLogService.recordForUser(
                9L,
                null,
                "auth.login",
                "auth",
                "9",
                "user@example.com",
                AuditLogService.RESULT_SUCCESS,
                null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(9L, saved.getUserId());
        assertEquals("auth.login", saved.getAction());
    }
}
