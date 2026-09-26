package com.boxai.user.application;

import com.boxai.common.result.PageResult;
import com.boxai.domain.audit.AuditLog;
import com.boxai.domain.audit.AuditLogQuery;
import com.boxai.domain.audit.AuditLogRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
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
class AdminAuditLogApplicationServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminAuditLogApplicationService service;

    @Test
    void pageDoesNotScopeWorkspaceAndParsesIsoTime() {
        AuditLog log = new AuditLog();
        log.setId(2L);
        log.setUserId(8L);
        log.setAction("auth.login");
        when(auditLogRepository.page(any())).thenReturn(new PageResult<>(List.of(log), 1, 1, 10));
        User user = new User();
        user.setId(8L);
        user.setEmail("admin@example.com");
        when(userRepository.findByIds(Set.of(8L))).thenReturn(List.of(user));

        var result = service.page(null, null, 8L, "2026-01-02T03:04:05", null, 1, 10);

        ArgumentCaptor<AuditLogQuery> captor = ArgumentCaptor.forClass(AuditLogQuery.class);
        verify(auditLogRepository).page(captor.capture());
        assertNull(captor.getValue().getWorkspaceId());
        assertEquals(LocalDateTime.of(2026, 1, 2, 3, 4, 5), captor.getValue().getStartTime());
        assertEquals("admin@example.com", result.records().get(0).userEmail());
        assertEquals("auth.login", result.records().get(0).action());
    }
}
