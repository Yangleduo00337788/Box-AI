package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.infrastructure.persistence.entity.WorkflowDO;
import com.boxai.infrastructure.persistence.mapper.WorkflowMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class WorkflowRepositoryImpl implements WorkflowRepository {

    private final WorkflowMapper mapper;

    public WorkflowRepositoryImpl(WorkflowMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Workflow save(Workflow workflow) {
        WorkflowDO row = toDo(workflow);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        workflow.setId(row.getId());
        workflow.setCreatedAt(row.getCreatedAt());
        workflow.setUpdatedAt(row.getUpdatedAt());
        return workflow;
    }

    @Override
    public void update(Workflow workflow) {
        WorkflowDO row = toDo(workflow);
        row.setId(workflow.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        workflow.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<Workflow> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<Workflow> listByWorkspace(Long workspaceId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create().eq("workspace_id", workspaceId).orderBy("updated_at", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private Workflow toDomain(WorkflowDO row) {
        Workflow workflow = new Workflow();
        workflow.setId(row.getId());
        workflow.setWorkspaceId(row.getWorkspaceId());
        workflow.setName(row.getName());
        workflow.setDescription(row.getDescription());
        workflow.setStatus(row.getStatus());
        workflow.setDraftVersionId(row.getDraftVersionId());
        workflow.setPublishedVersionId(row.getPublishedVersionId());
        workflow.setCreatedBy(row.getCreatedBy());
        workflow.setCreatedAt(row.getCreatedAt());
        workflow.setUpdatedAt(row.getUpdatedAt());
        return workflow;
    }

    private WorkflowDO toDo(Workflow workflow) {
        WorkflowDO row = new WorkflowDO();
        row.setWorkspaceId(workflow.getWorkspaceId());
        row.setName(workflow.getName());
        row.setDescription(workflow.getDescription());
        row.setStatus(workflow.getStatus() == null ? "DRAFT" : workflow.getStatus());
        row.setDraftVersionId(workflow.getDraftVersionId());
        row.setPublishedVersionId(workflow.getPublishedVersionId());
        row.setCreatedBy(workflow.getCreatedBy());
        return row;
    }
}
