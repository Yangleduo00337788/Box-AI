package com.boxai.trace.application;

import com.boxai.domain.trace.Execution;
import com.boxai.domain.trace.ExecutionRepository;
import com.boxai.domain.trace.Trace;
import com.boxai.domain.trace.TraceRepository;
import com.boxai.domain.trace.TraceSpan;
import com.boxai.domain.trace.TraceSpanRepository;
import com.boxai.security.context.WorkspaceContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExecutionRecorder {

    private final ExecutionRepository executionRepository;
    private final TraceRepository traceRepository;
    private final TraceSpanRepository traceSpanRepository;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, String> executionTraceIds = new ConcurrentHashMap<>();

    public ExecutionRecorder(ExecutionRepository executionRepository,
                             TraceRepository traceRepository,
                             TraceSpanRepository traceSpanRepository,
                             ObjectMapper objectMapper) {
        this.executionRepository = executionRepository;
        this.traceRepository = traceRepository;
        this.traceSpanRepository = traceSpanRepository;
        this.objectMapper = objectMapper;
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
        execution = executionRepository.save(execution);
        createTrace(execution, "Agent Execution");
        return execution;
    }

    public void recordRagSpan(Execution execution, Object input, Object output) {
        recordSpan(execution, "RAG", "Knowledge Retrieval", input, output, "SUCCEEDED", null);
    }

    public void recordLlmSpan(Execution execution, Object input, Object output) {
        recordSpan(execution, "LLM", "LLM Completion", input, output, "SUCCEEDED", null);
    }

    public void recordToolSpan(Execution execution, String toolKey, Object input, Object output) {
        recordSpan(execution, "TOOL", "Tool: " + toolKey, input, output, "SUCCEEDED", null);
    }

    public void recordToolSpanFailed(Execution execution, String toolKey, Object input, String errorMessage) {
        recordSpan(execution, "TOOL", "Tool: " + toolKey, input, null, "FAILED", errorMessage);
    }

    public void recordWorkflowNodeSpan(Execution execution,
                                       String nodeId,
                                       String nodeType,
                                       String status,
                                       Object output,
                                       String errorMessage) {
        String spanStatus = status == null ? "SUCCEEDED" : status;
        recordSpan(
                execution,
                "WORKFLOW_NODE",
                nodeType + " (" + nodeId + ")",
                Map.of("nodeId", nodeId, "nodeType", nodeType),
                output,
                spanStatus,
                errorMessage);
    }

    public void succeed(Execution execution, String outputJson, Integer totalTokens) {
        execution.setStatus("SUCCEEDED");
        execution.setOutputJson(outputJson);
        execution.setTotalTokens(totalTokens);
        execution.setFinishedAt(LocalDateTime.now());
        if (execution.getStartedAt() != null) {
            execution.setDurationMs(Duration.between(execution.getStartedAt(), execution.getFinishedAt()).toMillis());
        }
        executionRepository.update(execution);
        finishTrace(execution, "SUCCEEDED");
        executionTraceIds.remove(execution.getId());
    }

    public void fail(Execution execution, String errorMessage) {
        execution.setStatus("FAILED");
        execution.setErrorMessage(errorMessage);
        execution.setFinishedAt(LocalDateTime.now());
        if (execution.getStartedAt() != null) {
            execution.setDurationMs(Duration.between(execution.getStartedAt(), execution.getFinishedAt()).toMillis());
        }
        executionRepository.update(execution);
        finishTrace(execution, "FAILED");
        executionTraceIds.remove(execution.getId());
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
        execution = executionRepository.save(execution);
        createTrace(execution, "Workflow Execution");
        return execution;
    }

    private void createTrace(Execution execution, String name) {
        String traceId = "trc_" + UUID.randomUUID().toString().replace("-", "");
        Trace trace = new Trace();
        trace.setTraceId(traceId);
        trace.setExecutionId(execution.getId());
        trace.setWorkspaceId(execution.getWorkspaceId());
        trace.setName(name);
        trace.setStatus("RUNNING");
        trace.setStartTime(LocalDateTime.now());
        traceRepository.save(trace);
        executionTraceIds.put(execution.getId(), traceId);
    }

    private void finishTrace(Execution execution, String status) {
        traceRepository.findByExecutionId(execution.getId()).ifPresent(trace -> {
            trace.setStatus(status);
            trace.setEndTime(LocalDateTime.now());
            if (trace.getStartTime() != null) {
                trace.setDurationMs(Duration.between(trace.getStartTime(), trace.getEndTime()).toMillis());
            }
            traceRepository.update(trace);
        });
    }

    private void recordSpan(Execution execution,
                            String spanType,
                            String name,
                            Object input,
                            Object output,
                            String status,
                            String errorMessage) {
        String traceId = executionTraceIds.get(execution.getId());
        if (traceId == null) {
            traceId = traceRepository.findByExecutionId(execution.getId())
                    .map(Trace::getTraceId)
                    .orElse(null);
        }
        if (traceId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        TraceSpan span = new TraceSpan();
        span.setTraceId(traceId);
        span.setSpanId("spn_" + UUID.randomUUID().toString().replace("-", ""));
        span.setSpanType(spanType);
        span.setName(name);
        span.setStatus(status);
        span.setInputJson(toJson(input));
        span.setOutputJson(toJson(output));
        span.setStartTime(now);
        span.setEndTime(now);
        span.setDurationMs(0L);
        span.setErrorMessage(errorMessage);
        traceSpanRepository.save(span);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return objectMapper.createObjectNode()
                    .put("value", String.valueOf(value))
                    .toString();
        }
    }
}
