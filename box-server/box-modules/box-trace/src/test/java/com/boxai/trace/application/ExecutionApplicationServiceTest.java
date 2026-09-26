package com.boxai.trace.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.trace.Execution;
import com.boxai.domain.trace.ExecutionRepository;
import com.boxai.domain.trace.Trace;
import com.boxai.domain.trace.TraceRepository;
import com.boxai.domain.trace.TraceSpanRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionApplicationServiceTest {

    @Mock
    private ExecutionRepository executionRepository;
    @Mock
    private TraceRepository traceRepository;
    @Mock
    private TraceSpanRepository traceSpanRepository;
    @Mock
    private WorkspacePermissionService workspacePermissionService;

    @InjectMocks
    private ExecutionApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void listClampsLimit() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(executionRepository.listByWorkspace(7L, null, null, null, null, 50)).thenReturn(List.of());
        service.list(0);
        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_READ);
        verify(executionRepository).listByWorkspace(7L, null, null, null, null, 50);

        when(executionRepository.listByWorkspace(7L, "AGENT", "SUCCEEDED", 21L, 5L, 200)).thenReturn(List.of());
        service.list(999, "AGENT", "SUCCEEDED", 21L, 5L);
        verify(executionRepository).listByWorkspace(7L, "AGENT", "SUCCEEDED", 21L, 5L, 200);
    }

    @Test
    void detailRejectsCrossWorkspaceExecution() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Execution execution = new Execution();
        execution.setId(3L);
        execution.setWorkspaceId(99L);
        when(executionRepository.findById(3L)).thenReturn(Optional.of(execution));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.detail(3L));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
    }

    @Test
    void latestByConversationThrowsWhenEmpty() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(executionRepository.listByConversation(5L, 1)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.latestByConversation(5L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void traceRejectsMissingTrace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Execution execution = new Execution();
        execution.setId(3L);
        execution.setWorkspaceId(7L);
        when(executionRepository.findById(3L)).thenReturn(Optional.of(execution));
        when(traceRepository.findByExecutionId(3L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.trace(3L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }
}
