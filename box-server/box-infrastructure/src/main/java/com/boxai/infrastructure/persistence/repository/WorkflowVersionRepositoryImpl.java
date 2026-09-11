package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.workflow.WorkflowVersion;
import com.boxai.domain.workflow.WorkflowVersionRepository;
import com.boxai.infrastructure.persistence.entity.WorkflowVersionDO;
import com.boxai.infrastructure.persistence.mapper.WorkflowVersionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class WorkflowVersionRepositoryImpl implements WorkflowVersionRepository {

    private final WorkflowVersionMapper mapper;

    public WorkflowVersionRepositoryImpl(WorkflowVersionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public WorkflowVersion save(WorkflowVersion version) {
        WorkflowVersionDO row = toDo(version);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        version.setId(row.getId());
        version.setCreatedAt(row.getCreatedAt());
        return version;
    }

    @Override
    public void update(WorkflowVersion version) {
        WorkflowVersionDO row = toDo(version);
        row.setId(version.getId());
        mapper.update(row);
    }

    @Override
    public Optional<WorkflowVersion> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<WorkflowVersion> findLatestDraft(Long workflowId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("workflow_id", workflowId)
                                .eq("status", "DRAFT")
                                .orderBy("version_no", false)
                                .limit(1))
                .stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public Optional<Integer> findMaxVersionNo(Long workflowId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create().eq("workflow_id", workflowId).orderBy("version_no", false).limit(1))
                .stream()
                .findFirst()
                .map(WorkflowVersionDO::getVersionNo);
    }

    @Override
    public void deleteByWorkflowId(Long workflowId) {
        mapper.deleteByQuery(QueryWrapper.create().eq("workflow_id", workflowId));
    }

    private WorkflowVersion toDomain(WorkflowVersionDO row) {
        WorkflowVersion version = new WorkflowVersion();
        version.setId(row.getId());
        version.setWorkflowId(row.getWorkflowId());
        version.setWorkspaceId(row.getWorkspaceId());
        version.setVersionNo(row.getVersionNo());
        version.setDefinitionJson(row.getDefinition());
        version.setStatus(row.getStatus());
        version.setChangeLog(row.getChangeLog());
        version.setCreatedBy(row.getCreatedBy());
        version.setCreatedAt(row.getCreatedAt());
        return version;
    }

    private WorkflowVersionDO toDo(WorkflowVersion version) {
        WorkflowVersionDO row = new WorkflowVersionDO();
        row.setWorkflowId(version.getWorkflowId());
        row.setWorkspaceId(version.getWorkspaceId());
        row.setVersionNo(version.getVersionNo());
        row.setDefinition(version.getDefinitionJson());
        row.setStatus(version.getStatus() == null ? "DRAFT" : version.getStatus());
        row.setChangeLog(version.getChangeLog());
        row.setCreatedBy(version.getCreatedBy());
        return row;
    }
}
