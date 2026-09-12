package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.trace.Execution;
import com.boxai.domain.trace.ExecutionRepository;
import com.boxai.infrastructure.persistence.entity.ExecutionDO;
import com.boxai.infrastructure.persistence.mapper.ExecutionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ExecutionRepositoryImpl implements ExecutionRepository {

    private final ExecutionMapper mapper;

    public ExecutionRepositoryImpl(ExecutionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Execution save(Execution execution) {
        ExecutionDO row = toDo(execution);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        execution.setId(row.getId());
        execution.setCreatedAt(row.getCreatedAt());
        return execution;
    }

    @Override
    public void update(Execution execution) {
        ExecutionDO row = toDo(execution);
        row.setId(execution.getId());
        mapper.update(row);
    }

    @Override
    public Optional<Execution> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<Execution> listByWorkspace(Long workspaceId, int limit) {
        return listByWorkspace(workspaceId, null, null, null, null, limit);
    }

    @Override
    public List<Execution> listByWorkspace(Long workspaceId,
                                           String executionType,
                                           String status,
                                           Long agentId,
                                           Long conversationId,
                                           int limit) {
        QueryWrapper query = QueryWrapper.create()
                .eq("workspace_id", workspaceId)
                .orderBy("created_at", false)
                .limit(Math.max(limit, 1));
        if (executionType != null && !executionType.isBlank()) {
            query.eq("execution_type", executionType.trim().toUpperCase());
        }
        if (status != null && !status.isBlank()) {
            query.eq("status", status.trim().toUpperCase());
        }
        if (agentId != null) {
            query.eq("agent_id", agentId);
        }
        if (conversationId != null) {
            query.eq("conversation_id", conversationId);
        }
        return mapper.selectListByQuery(query).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Execution> listByConversation(Long conversationId, int limit) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("conversation_id", conversationId)
                                .orderBy("created_at", false)
                                .limit(Math.max(limit, 1)))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countByWorkspace(Long workspaceId) {
        Long count = mapper.selectCountByQuery(QueryWrapper.create().eq("workspace_id", workspaceId));
        return count == null ? 0 : count.intValue();
    }

    private Execution toDomain(ExecutionDO row) {
        Execution execution = new Execution();
        execution.setId(row.getId());
        execution.setExecutionNo(row.getExecutionNo());
        execution.setWorkspaceId(row.getWorkspaceId());
        execution.setExecutionType(row.getExecutionType());
        execution.setAgentId(row.getAgentId());
        execution.setAgentVersionId(row.getAgentVersionId());
        execution.setWorkflowId(row.getWorkflowId());
        execution.setWorkflowVersionId(row.getWorkflowVersionId());
        execution.setConversationId(row.getConversationId());
        execution.setUserId(row.getUserId());
        execution.setStatus(row.getStatus());
        execution.setInputJson(row.getInputJson());
        execution.setOutputJson(row.getOutputJson());
        execution.setErrorCode(row.getErrorCode());
        execution.setErrorMessage(row.getErrorMessage());
        execution.setStartedAt(row.getStartedAt());
        execution.setFinishedAt(row.getFinishedAt());
        execution.setDurationMs(row.getDurationMs());
        execution.setTotalTokens(row.getTotalTokens());
        execution.setCreatedAt(row.getCreatedAt());
        return execution;
    }

    private ExecutionDO toDo(Execution execution) {
        ExecutionDO row = new ExecutionDO();
        row.setExecutionNo(execution.getExecutionNo());
        row.setWorkspaceId(execution.getWorkspaceId());
        row.setExecutionType(execution.getExecutionType());
        row.setAgentId(execution.getAgentId());
        row.setAgentVersionId(execution.getAgentVersionId());
        row.setWorkflowId(execution.getWorkflowId());
        row.setWorkflowVersionId(execution.getWorkflowVersionId());
        row.setConversationId(execution.getConversationId());
        row.setUserId(execution.getUserId());
        row.setStatus(execution.getStatus());
        row.setInputJson(execution.getInputJson());
        row.setOutputJson(execution.getOutputJson());
        row.setErrorCode(execution.getErrorCode());
        row.setErrorMessage(execution.getErrorMessage());
        row.setStartedAt(execution.getStartedAt());
        row.setFinishedAt(execution.getFinishedAt());
        row.setDurationMs(execution.getDurationMs());
        row.setTotalTokens(execution.getTotalTokens());
        return row;
    }
}
