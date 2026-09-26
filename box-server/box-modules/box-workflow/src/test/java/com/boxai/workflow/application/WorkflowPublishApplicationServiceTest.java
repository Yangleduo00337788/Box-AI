package com.boxai.workflow.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.constant.PublishResourceTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.publish.Publish;
import com.boxai.domain.publish.PublishRepository;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.domain.workflow.WorkflowVersion;
import com.boxai.domain.workflow.WorkflowVersionRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.workflow.api.WorkflowValidateVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowPublishApplicationServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;
    @Mock
    private WorkflowVersionRepository workflowVersionRepository;
    @Mock
    private WorkflowApplicationService workflowApplicationService;
    @Mock
    private WorkflowDefinitionValidator workflowDefinitionValidator;
    @Mock
    private PublishRepository publishRepository;
    @Mock
    private WorkspacePermissionService workspacePermissionService;

    @InjectMocks
    private WorkflowPublishApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void publishRejectsInvalidDefinition() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Workflow workflow = workflow();
        WorkflowVersion draft = draft();
        when(workflowApplicationService.requireWorkflow(5L)).thenReturn(workflow);
        when(workflowApplicationService.requireDraft(workflow)).thenReturn(draft);
        when(workflowDefinitionValidator.validate("{}", 7L))
                .thenReturn(new WorkflowValidateVO(false, List.of("缺少结束节点")));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.publish(5L));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(workflowRepository, never()).update(any());
    }

    @Test
    void publishCreatesWebhookAndNewDraft() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Workflow workflow = workflow();
        WorkflowVersion draft = draft();
        when(workflowApplicationService.requireWorkflow(5L)).thenReturn(workflow);
        when(workflowApplicationService.requireDraft(workflow)).thenReturn(draft);
        when(workflowDefinitionValidator.validate("{}", 7L)).thenReturn(new WorkflowValidateVO(true, List.of()));
        when(workflowVersionRepository.findMaxVersionNo(5L)).thenReturn(Optional.of(1));
        doAnswer(invocation -> {
            WorkflowVersion version = invocation.getArgument(0);
            version.setId(12L);
            return version;
        }).when(workflowVersionRepository).save(any(WorkflowVersion.class));
        when(workflowVersionRepository.findById(8L)).thenReturn(Optional.of(draft));

        var vo = service.publish(5L);

        verify(workspacePermissionService).requirePermission(PermissionCodes.WORKFLOW_UPDATE);
        assertEquals("PUBLISHED", draft.getStatus());
        assertEquals(8L, workflow.getPublishedVersionId());
        assertEquals(12L, workflow.getDraftVersionId());
        assertNotNull(workflow.getWebhookToken());
        assertNotNull(workflow.getWebhookSecret());
        verify(publishRepository).revokeByResource(PublishResourceTypes.WORKFLOW, 5L);
        ArgumentCaptor<Publish> captor = ArgumentCaptor.forClass(Publish.class);
        verify(publishRepository).save(captor.capture());
        assertEquals(8L, captor.getValue().getVersionId());
        assertEquals("PUBLISHED", vo.status());
        assertEquals(8L, vo.publishedVersionId());
        assertTrue(vo.webhookUrl().startsWith("/api/v1/hooks/workflows/"));
    }

    @Test
    void getPublishStatusReturnsEmptyWhenUnpublished() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Workflow workflow = workflow();
        workflow.setPublishedVersionId(null);
        when(workflowApplicationService.requireWorkflow(5L)).thenReturn(workflow);

        var vo = service.getPublishStatus(5L);

        verify(workspacePermissionService).requirePermission(PermissionCodes.WORKFLOW_READ);
        assertEquals("DRAFT", vo.status());
        assertEquals(null, vo.publishedVersionId());
    }

    private static Workflow workflow() {
        Workflow workflow = new Workflow();
        workflow.setId(5L);
        workflow.setWorkspaceId(7L);
        workflow.setStatus("DRAFT");
        workflow.setDraftVersionId(8L);
        return workflow;
    }

    private static WorkflowVersion draft() {
        WorkflowVersion draft = new WorkflowVersion();
        draft.setId(8L);
        draft.setWorkflowId(5L);
        draft.setWorkspaceId(7L);
        draft.setVersionNo(1);
        draft.setDefinitionJson("{}");
        return draft;
    }
}
