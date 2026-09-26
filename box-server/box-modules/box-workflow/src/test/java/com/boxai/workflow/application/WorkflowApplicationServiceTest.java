package com.boxai.workflow.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.domain.workflow.WorkflowVersion;
import com.boxai.domain.workflow.WorkflowVersionRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.guard.ResourceDeleteGuard;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.workflow.api.CreateWorkflowRequest;
import com.boxai.workflow.api.UpdateWorkflowDefinitionRequest;
import com.boxai.workflow.api.UpdateWorkflowRequest;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowApplicationServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;
    @Mock
    private WorkflowVersionRepository workflowVersionRepository;
    @Mock
    private WorkflowDefinitionValidator workflowDefinitionValidator;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private ResourceDeleteGuard resourceDeleteGuard;

    @InjectMocks
    private WorkflowApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createSavesDraftWorkflowAndVersion() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(workflowDefinitionValidator.normalizeDefinition(null)).thenReturn("{\"nodes\":[]}");
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> {
            Workflow workflow = invocation.getArgument(0);
            workflow.setId(5L);
            return workflow;
        });
        when(workflowVersionRepository.save(any(WorkflowVersion.class))).thenAnswer(invocation -> {
            WorkflowVersion version = invocation.getArgument(0);
            version.setId(8L);
            return version;
        });
        when(workflowVersionRepository.findById(8L)).thenAnswer(invocation -> {
            WorkflowVersion version = new WorkflowVersion();
            version.setId(8L);
            version.setVersionNo(1);
            version.setDefinitionJson("{\"nodes\":[]}");
            return Optional.of(version);
        });

        var vo = service.create(new CreateWorkflowRequest(" 退款流 ", "  "));

        verify(workspacePermissionService).requirePermission(PermissionCodes.WORKFLOW_CREATE);
        ArgumentCaptor<Workflow> workflowCaptor = ArgumentCaptor.forClass(Workflow.class);
        verify(workflowRepository).save(workflowCaptor.capture());
        assertEquals("退款流", workflowCaptor.getValue().getName());
        assertEquals("DRAFT", workflowCaptor.getValue().getStatus());
        assertEquals(7L, workflowCaptor.getValue().getWorkspaceId());
        ArgumentCaptor<WorkflowVersion> versionCaptor = ArgumentCaptor.forClass(WorkflowVersion.class);
        verify(workflowVersionRepository).save(versionCaptor.capture());
        assertEquals(5L, versionCaptor.getValue().getWorkflowId());
        assertEquals(1, versionCaptor.getValue().getVersionNo());
        verify(workflowRepository).update(any(Workflow.class));
        assertEquals(5L, vo.id());
        assertEquals(8L, vo.draftVersionId());
        assertEquals("{\"nodes\":[]}", vo.definitionJson());
    }

    @Test
    void requireWorkflowRejectsOtherWorkspace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Workflow workflow = new Workflow();
        workflow.setId(5L);
        workflow.setWorkspaceId(99L);
        when(workflowRepository.findById(5L)).thenReturn(Optional.of(workflow));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.detail(5L));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
    }

    @Test
    void updateDefinitionWritesNormalizedJson() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Workflow workflow = ownedWorkflow();
        WorkflowVersion draft = new WorkflowVersion();
        draft.setId(8L);
        draft.setVersionNo(1);
        when(workflowRepository.findById(5L)).thenReturn(Optional.of(workflow));
        when(workflowVersionRepository.findById(8L)).thenReturn(Optional.of(draft));
        when(workflowDefinitionValidator.normalizeDefinition("raw")).thenReturn("normalized");

        var vo = service.updateDefinition(5L, new UpdateWorkflowDefinitionRequest("raw"));

        verify(workspacePermissionService).requirePermission(PermissionCodes.WORKFLOW_UPDATE);
        assertEquals("normalized", draft.getDefinitionJson());
        verify(workflowVersionRepository).update(draft);
        assertEquals("normalized", vo.definitionJson());
    }

    @Test
    void validateUsesDraftDefinition() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Workflow workflow = ownedWorkflow();
        WorkflowVersion draft = new WorkflowVersion();
        draft.setId(8L);
        draft.setDefinitionJson("{}");
        when(workflowRepository.findById(5L)).thenReturn(Optional.of(workflow));
        when(workflowVersionRepository.findById(8L)).thenReturn(Optional.of(draft));
        when(workflowDefinitionValidator.validate("{}", 7L))
                .thenReturn(new WorkflowValidateVO(true, List.of()));

        var vo = service.validate(5L);

        verify(workspacePermissionService).requirePermission(PermissionCodes.WORKFLOW_READ);
        assertEquals(true, vo.valid());
    }

    @Test
    void deleteClearsVersionsWhenGuardAllows() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Workflow workflow = ownedWorkflow();
        when(workflowRepository.findById(5L)).thenReturn(Optional.of(workflow));

        service.delete(5L);

        verify(resourceDeleteGuard).assertWorkflowDeletable(workflow);
        verify(workflowVersionRepository).deleteByWorkflowId(5L);
        verify(workflowRepository).delete(5L);
    }

    @Test
    void updateTrimsNameAndBlankDescription() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Workflow workflow = ownedWorkflow();
        when(workflowRepository.findById(5L)).thenReturn(Optional.of(workflow));
        when(workflowVersionRepository.findById(8L)).thenReturn(Optional.empty());
        when(workflowVersionRepository.findLatestDraft(5L)).thenReturn(Optional.empty());

        service.update(5L, new UpdateWorkflowRequest(" 新名称 ", "   "));

        assertEquals("新名称", workflow.getName());
        assertNull(workflow.getDescription());
        verify(workflowRepository).update(workflow);
    }

    @Test
    void listOmitsDefinitionJson() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Workflow workflow = ownedWorkflow();
        when(workflowRepository.listByWorkspace(7L)).thenReturn(List.of(workflow));
        when(workflowVersionRepository.findById(8L)).thenReturn(Optional.empty());
        when(workflowVersionRepository.findLatestDraft(5L)).thenReturn(Optional.empty());

        var vos = service.list();

        verify(workspacePermissionService).requirePermission(PermissionCodes.WORKFLOW_READ);
        assertEquals(1, vos.size());
        assertNull(vos.get(0).definitionJson());
        assertEquals("退款流", vos.get(0).name());
    }

    private static Workflow ownedWorkflow() {
        Workflow workflow = new Workflow();
        workflow.setId(5L);
        workflow.setWorkspaceId(7L);
        workflow.setName("退款流");
        workflow.setStatus("DRAFT");
        workflow.setDraftVersionId(8L);
        return workflow;
    }
}
