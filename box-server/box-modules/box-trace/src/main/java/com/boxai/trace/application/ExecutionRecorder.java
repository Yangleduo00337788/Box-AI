package com.boxai.trace.application;

import com.boxai.domain.trace.Execution;
import com.boxai.domain.trace.ExecutionRepository;
import com.boxai.security.context.WorkspaceContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ExecutionRecorder {

    private final ExecutionRepository executionRepository;

    public ExecutionRecorder(ExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    public Execution startAgentExecution(Long agentId, Long agentVersionId, Long conversationId, String inputJson) {
        Execution execution = new Execution();
        execution.setExecutionNo("exe_" + UUID.randomUUID().toString().replace("-", ""));
        execution.setWorkspaceId(WorkspaceContext.require().workspaceId());
        execution.setExecutionType("AGENT");
        execution.setAgentId(agentId);
        execution.setAgentVersionId(agentVersionId);
        execution.setConversationId(conversationId);
        execution.setUserId(WorkspaceContext.require().userId());
        execution.setStatus("RUNNING");
        execution.setInputJson(inputJson);
        execution.setStartedAt(LocalDateTime.now());
        return executionRepository.save(execution);
    }

    public void succeed(Execution execution, String outputJson, Integer totalTokens) {
        execution.setStatus("SUCCEEDED");
        execution.setOutputJson(outputJson);
        execution.setTotalTokens(totalTokens);
        execution.setFinishedAt(LocalDateTime.now());
        if (execution.getStartedAt() != null) {
            execution.setDurationMs(
                    java.time.Duration.between(execution.getStartedAt(), execution.getFinishedAt()).toMillis());
        }
        executionRepository.update(execution);
    }

    public void fail(Execution execution, String errorMessage) {
        execution.setStatus("FAILED");
        execution.setErrorMessage(errorMessage);
        execution.setFinishedAt(LocalDateTime.now());
        if (execution.getStartedAt() != null) {
            execution.setDurationMs(
                    java.time.Duration.between(execution.getStartedAt(), execution.getFinishedAt()).toMillis());
        }
        executionRepository.update(execution);
    }

    public Execution startWorkflowExecution(Long workflowId, Long workflowVersionId, String inputJson) {
        Execution execution = new Execution();
        execution.setExecutionNo("exe_" + UUID.randomUUID().toString().replace("-", ""));
        execution.setWorkspaceId(WorkspaceContext.require().workspaceId());
        execution.setExecutionType("WORKFLOW");
        execution.setWorkflowId(workflowId);
        execution.setWorkflowVersionId(workflowVersionId);
        execution.setUserId(WorkspaceContext.require().userId());
        execution.setStatus("RUNNING");
        execution.setInputJson(inputJson);
        execution.setStartedAt(LocalDateTime.now());
        return executionRepository.save(execution);
    }
}
