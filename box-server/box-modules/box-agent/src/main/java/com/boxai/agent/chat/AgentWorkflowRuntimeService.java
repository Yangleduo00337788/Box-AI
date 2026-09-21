package com.boxai.agent.chat;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.AgentWorkflow;
import com.boxai.domain.agent.AgentWorkflowRepository;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentWorkflowRuntimeService {

    private final AgentWorkflowRepository agentWorkflowRepository;
    private final WorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper;
    private final AgentWorkflowInvoker workflowInvoker;

    public AgentWorkflowRuntimeService(AgentWorkflowRepository agentWorkflowRepository,
                                       WorkflowRepository workflowRepository,
                                       ObjectMapper objectMapper,
                                       @Lazy @Autowired(required = false) AgentWorkflowInvoker workflowInvoker) {
        this.agentWorkflowRepository = agentWorkflowRepository;
        this.workflowRepository = workflowRepository;
        this.objectMapper = objectMapper;
        this.workflowInvoker = workflowInvoker;
    }

    public String runDefaultIfPresent(Long versionId, String userMessage) {
        AgentWorkflow binding = agentWorkflowRepository.findDefaultByVersionId(versionId).orElse(null);
        if (binding == null) {
            return null;
        }
        return invoke(binding.getWorkflowId(), userMessage);
    }

    public String invoke(Long workflowId, Map<String, Object> arguments) {
        if (workflowInvoker == null) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "工作流运行时未就绪");
        }
        Map<String, Object> inputs = arguments == null ? Map.of() : new LinkedHashMap<>(arguments);
        if (!inputs.containsKey("query") && !inputs.containsKey("message")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流调用缺少 query 或 message 参数");
        }
        return workflowInvoker.invokeDraft(workflowId, inputs);
    }

    public String invoke(Long workflowId, String userMessage) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("query", userMessage);
        inputs.put("message", userMessage);
        return invoke(workflowId, inputs);
    }

    public List<ResolvedAgentTool> resolveCallableTools(Long versionId) {
        return agentWorkflowRepository.listByVersionId(versionId).stream()
                .filter(binding -> Boolean.TRUE.equals(binding.getEnabled()))
                .filter(binding -> Boolean.TRUE.equals(binding.getCallable()))
                .map(binding -> {
                    Workflow workflow = workflowRepository.findById(binding.getWorkflowId()).orElse(null);
                    if (workflow == null) {
                        return null;
                    }
                    String description = workflow.getDescription() == null || workflow.getDescription().isBlank()
                            ? "执行工作流 " + workflow.getName()
                            : workflow.getDescription();
                    return new ResolvedAgentTool(
                            null,
                            workflowToolKey(workflow.getId()),
                            workflow.getName(),
                            description,
                            "WORKFLOW",
                            null,
                            null,
                            null,
                            workflow.getId(),
                            false);
                })
                .filter(item -> item != null)
                .toList();
    }

    public static String workflowToolKey(Long workflowId) {
        return "workflow_" + workflowId;
    }

    private String extractMessage(Map<String, Object> arguments) {
        Object message = arguments.containsKey("message") ? arguments.get("message") : arguments.get("query");
        if (message == null || String.valueOf(message).isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流调用缺少 message 参数");
        }
        return String.valueOf(message);
    }
}
