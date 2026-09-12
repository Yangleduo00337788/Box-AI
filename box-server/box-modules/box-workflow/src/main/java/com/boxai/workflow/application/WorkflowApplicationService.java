package com.boxai.workflow.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.domain.workflow.WorkflowVersion;
import com.boxai.domain.workflow.WorkflowVersionRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.workflow.api.CreateWorkflowRequest;
import com.boxai.workflow.api.UpdateWorkflowDefinitionRequest;
import com.boxai.workflow.api.UpdateWorkflowRequest;
import com.boxai.workflow.api.WorkflowVO;
import com.boxai.workflow.api.WorkflowValidateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkflowApplicationService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowDefinitionValidator workflowDefinitionValidator;
    private final WorkspacePermissionService workspacePermissionService;

    public WorkflowApplicationService(WorkflowRepository workflowRepository,
                                      WorkflowVersionRepository workflowVersionRepository,
                                      WorkflowDefinitionValidator workflowDefinitionValidator,
                                      WorkspacePermissionService workspacePermissionService) {
        this.workflowRepository = workflowRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.workflowDefinitionValidator = workflowDefinitionValidator;
        this.workspacePermissionService = workspacePermissionService;
    }

    public List<WorkflowVO> list() {
        workspacePermissionService.requirePermission("workflow:execute");
        return workflowRepository.listByWorkspace(workspaceId()).stream()
                .map(workflow -> toVO(workflow, false))
                .toList();
    }

    public WorkflowVO detail(Long id) {
        workspacePermissionService.requirePermission("workflow:execute");
        return toVO(requireWorkflow(id), true);
    }

    @Transactional
    public WorkflowVO create(CreateWorkflowRequest request) {
        workspacePermissionService.requirePermission("workflow:create");
        Long userId = WorkspaceContext.require().userId();
        Workflow workflow = new Workflow();
        workflow.setWorkspaceId(workspaceId());
        workflow.setName(request.name().trim());
        workflow.setDescription(trimToNull(request.description()));
        workflow.setStatus("DRAFT");
        workflow.setCreatedBy(userId);
        workflowRepository.save(workflow);

        WorkflowVersion version = new WorkflowVersion();
        version.setWorkflowId(workflow.getId());
        version.setWorkspaceId(workflow.getWorkspaceId());
        version.setVersionNo(1);
        version.setStatus("DRAFT");
        version.setDefinitionJson(workflowDefinitionValidator.normalizeDefinition(null));
        version.setCreatedBy(userId);
        workflowVersionRepository.save(version);

        workflow.setDraftVersionId(version.getId());
        workflowRepository.update(workflow);
        return toVO(workflow, true);
    }

    @Transactional
    public WorkflowVO update(Long id, UpdateWorkflowRequest request) {
        workspacePermissionService.requirePermission("workflow:update");
        Workflow workflow = requireWorkflow(id);
        workflow.setName(request.name().trim());
        workflow.setDescription(trimToNull(request.description()));
        workflowRepository.update(workflow);
        return toVO(workflow, true);
    }

    @Transactional
    public WorkflowVO updateDefinition(Long id, UpdateWorkflowDefinitionRequest request) {
        workspacePermissionService.requirePermission("workflow:update");
        Workflow workflow = requireWorkflow(id);
        WorkflowVersion draft = requireDraft(workflow);
        String definitionJson = workflowDefinitionValidator.normalizeDefinition(request.definitionJson());
        draft.setDefinitionJson(definitionJson);
        workflowVersionRepository.update(draft);
        workflowRepository.update(workflow);
        return toVO(workflow, true);
    }

    public WorkflowValidateVO validate(Long id) {
        workspacePermissionService.requirePermission("workflow:execute");
        Workflow workflow = requireWorkflow(id);
        WorkflowVersion draft = requireDraft(workflow);
        return workflowDefinitionValidator.validate(draft.getDefinitionJson());
    }

    @Transactional
    public void delete(Long id) {
        workspacePermissionService.requirePermission("workflow:update");
        Workflow workflow = requireWorkflow(id);
        workflowVersionRepository.deleteByWorkflowId(workflow.getId());
        workflowRepository.delete(workflow.getId());
    }

    public Workflow requireWorkflow(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_NOT_FOUND, "工作流不存在"));
        if (!workspaceId().equals(workflow.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该工作流");
        }
        return workflow;
    }

    public WorkflowVersion requireDraft(Workflow workflow) {
        Long draftVersionId = workflow.getDraftVersionId();
        if (draftVersionId != null) {
            return workflowVersionRepository.findById(draftVersionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_VERSION_NOT_FOUND, "工作流草稿版本不存在"));
        }
        return workflowVersionRepository.findLatestDraft(workflow.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_VERSION_NOT_FOUND, "工作流草稿版本不存在"));
    }

    private WorkflowVO toVO(Workflow workflow, boolean includeDefinition) {
        WorkflowVersion draft = null;
        if (workflow.getDraftVersionId() != null) {
            draft = workflowVersionRepository.findById(workflow.getDraftVersionId()).orElse(null);
        }
        if (draft == null) {
            draft = workflowVersionRepository.findLatestDraft(workflow.getId()).orElse(null);
        }

        WorkflowVersion published = null;
        if (workflow.getPublishedVersionId() != null) {
            published = workflowVersionRepository.findById(workflow.getPublishedVersionId()).orElse(null);
        }

        return new WorkflowVO(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getStatus(),
                draft == null ? workflow.getDraftVersionId() : draft.getId(),
                workflow.getPublishedVersionId(),
                draft == null ? null : draft.getVersionNo(),
                published == null ? null : published.getVersionNo(),
                includeDefinition && draft != null ? draft.getDefinitionJson() : null,
                workflow.getCreatedAt(),
                workflow.getUpdatedAt());
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
