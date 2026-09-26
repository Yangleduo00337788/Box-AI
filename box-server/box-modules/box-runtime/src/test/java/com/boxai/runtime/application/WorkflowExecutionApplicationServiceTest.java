package com.boxai.runtime.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.trace.Execution;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowVersion;
import com.boxai.domain.workflow.WorkflowVersionRepository;
import com.boxai.runtime.api.WorkflowExecuteRequest;
import com.boxai.runtime.workflow.core.WorkflowExecutionResult;
import com.boxai.runtime.workflow.engine.DefaultWorkflowExecutor;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.notification.NotificationPublisher;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tenant.application.QuotaApplicationService;
import com.boxai.trace.application.ExecutionRecorder;
import com.boxai.workflow.api.WorkflowValidateVO;
import com.boxai.workflow.application.WorkflowApplicationService;
import com.boxai.workflow.application.WorkflowDefinitionValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutionApplicationServiceTest {

    @Mock
    private WorkflowVersionRepository workflowVersionRepository;
    @Mock
    private WorkflowApplicationService workflowApplicationService;
    @Mock
    private WorkflowDefinitionValidator workflowDefinitionValidator;
    @Mock
    private DefaultWorkflowExecutor workflowExecutor;
    @Mock
    private ExecutionRecorder executionRecorder;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private QuotaApplicationService quotaApplicationService;
    @Mock
    private NotificationPublisher notificationPublisher;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void debugRejectsInvalidDefinition() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Workflow workflow = workflow();
        WorkflowVersion version = version();
        when(workflowApplicationService.requireWorkflow(4L)).thenReturn(workflow);
        when(workflowApplicationService.requireDraft(workflow)).thenReturn(version);
        when(workflowDefinitionValidator.validate(eq(version.getDefinitionJson()), eq(7L)))
                .thenReturn(new WorkflowValidateVO(false, List.of("必须包含一个 Start 节点")));

        WorkflowExecutionApplicationService service = newService();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.debug(4L, new WorkflowExecuteRequest(Map.of())));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(workspacePermissionService).requirePermission(PermissionCodes.WORKFLOW_EXECUTE);
        verify(quotaApplicationService, never()).assertAiQuotaAvailable(any());
    }

    @Test
    void debugSucceedsAndRecordsExecution() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Workflow workflow = workflow();
        WorkflowVersion version = version();
        when(workflowApplicationService.requireWorkflow(4L)).thenReturn(workflow);
        when(workflowApplicationService.requireDraft(workflow)).thenReturn(version);
        when(workflowDefinitionValidator.validate(eq(version.getDefinitionJson()), eq(7L)))
                .thenReturn(new WorkflowValidateVO(true, List.of()));
        Execution execution = new Execution();
        execution.setId(80L);
        execution.setExecutionNo("wf-80");
        when(executionRecorder.startWorkflowExecution(eq(4L), eq(40L), any())).thenReturn(execution);
        when(workflowExecutor.execute(eq(version.getDefinitionJson()), any(), isNull()))
                .thenReturn(new WorkflowExecutionResult(80L, "wf-80", "SUCCEEDED", Map.of("out", 1), List.of(), null));

        var result = newService().debug(4L, new WorkflowExecuteRequest(Map.of("q", "hi")));

        verify(quotaApplicationService).assertAiQuotaAvailable(7L);
        verify(executionRecorder).succeed(eq(execution), any(), isNull());
        verify(notificationPublisher, never()).publish(any(), any(), any(), any(), any(), any());
        assertEquals("SUCCEEDED", result.status());
        assertEquals(4L, result.workflowId());
    }

    private WorkflowExecutionApplicationService newService() {
        return new WorkflowExecutionApplicationService(
                workflowVersionRepository,
                workflowApplicationService,
                workflowDefinitionValidator,
                workflowExecutor,
                executionRecorder,
                new ObjectMapper(),
                workspacePermissionService,
                quotaApplicationService,
                notificationPublisher);
    }

    private static Workflow workflow() {
        Workflow workflow = new Workflow();
        workflow.setId(4L);
        workflow.setWorkspaceId(7L);
        workflow.setName("Flow");
        return workflow;
    }

    private static WorkflowVersion version() {
        WorkflowVersion version = new WorkflowVersion();
        version.setId(40L);
        version.setDefinitionJson("{\"nodes\":[]}");
        return version;
    }
}
