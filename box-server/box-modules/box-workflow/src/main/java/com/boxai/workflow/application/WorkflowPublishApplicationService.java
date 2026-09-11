package com.boxai.workflow.application;

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
import com.boxai.workflow.api.WorkflowPublishVO;
import com.boxai.workflow.api.WorkflowValidateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class WorkflowPublishApplicationService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowApplicationService workflowApplicationService;
    private final WorkflowDefinitionValidator workflowDefinitionValidator;
    private final PublishRepository publishRepository;

    public WorkflowPublishApplicationService(WorkflowRepository workflowRepository,
                                             WorkflowVersionRepository workflowVersionRepository,
                                             WorkflowApplicationService workflowApplicationService,
                                             WorkflowDefinitionValidator workflowDefinitionValidator,
                                             PublishRepository publishRepository) {
        this.workflowRepository = workflowRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.workflowApplicationService = workflowApplicationService;
        this.workflowDefinitionValidator = workflowDefinitionValidator;
        this.publishRepository = publishRepository;
    }

    public WorkflowPublishVO getPublishStatus(Long workflowId) {
        Workflow workflow = workflowApplicationService.requireWorkflow(workflowId);
        if (workflow.getPublishedVersionId() == null) {
            return new WorkflowPublishVO(workflow.getId(), workflow.getStatus(), null, null, null);
        }
        WorkflowVersion published = workflowVersionRepository.findById(workflow.getPublishedVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_VERSION_NOT_FOUND, "发布版本不存在"));
        return new WorkflowPublishVO(
                workflow.getId(),
                workflow.getStatus(),
                published.getId(),
                published.getVersionNo(),
                published.getCreatedAt());
    }

    @Transactional
    public WorkflowPublishVO publish(Long workflowId) {
        Workflow workflow = workflowApplicationService.requireWorkflow(workflowId);
        WorkflowVersion draft = workflowApplicationService.requireDraft(workflow);
        WorkflowValidateVO validation = workflowDefinitionValidator.validate(draft.getDefinitionJson());
        if (!validation.valid()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, String.join("；", validation.errors()));
        }

        draft.setStatus("PUBLISHED");
        workflowVersionRepository.update(draft);

        workflow.setPublishedVersionId(draft.getId());
        workflow.setStatus("PUBLISHED");
        workflowRepository.update(workflow);

        int nextVersionNo = workflowVersionRepository.findMaxVersionNo(workflow.getId()).orElse(0) + 1;
        WorkflowVersion newDraft = copyVersion(draft, nextVersionNo, "DRAFT");
        workflowVersionRepository.save(newDraft);
        workflow.setDraftVersionId(newDraft.getId());
        workflowRepository.update(workflow);

        publishRepository.revokeByResource(PublishResourceTypes.WORKFLOW, workflow.getId());
        Publish publish = new Publish();
        publish.setWorkspaceId(workflow.getWorkspaceId());
        publish.setResourceType(PublishResourceTypes.WORKFLOW);
        publish.setResourceId(workflow.getId());
        publish.setVersionId(draft.getId());
        publish.setChannel("API");
        publish.setStatus("PUBLISHED");
        publish.setPublishedBy(WorkspaceContext.require().userId());
        publish.setPublishedAt(LocalDateTime.now());
        publishRepository.save(publish);

        return getPublishStatus(workflowId);
    }

    private WorkflowVersion copyVersion(WorkflowVersion source, int versionNo, String status) {
        WorkflowVersion version = new WorkflowVersion();
        version.setWorkflowId(source.getWorkflowId());
        version.setWorkspaceId(source.getWorkspaceId());
        version.setVersionNo(versionNo);
        version.setStatus(status);
        version.setDefinitionJson(source.getDefinitionJson());
        version.setChangeLog(source.getChangeLog());
        version.setCreatedBy(WorkspaceContext.require().userId());
        return version;
    }
}
