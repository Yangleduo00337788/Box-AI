package com.boxai.runtime.application;

import com.boxai.agent.chat.AgentWorkflowInvoker;
import com.boxai.runtime.api.WorkflowExecuteRequest;
import com.boxai.runtime.api.WorkflowExecutionResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DefaultAgentWorkflowInvoker implements AgentWorkflowInvoker {

    private final WorkflowExecutionApplicationService workflowExecutionApplicationService;
    private final ObjectMapper objectMapper;

    public DefaultAgentWorkflowInvoker(WorkflowExecutionApplicationService workflowExecutionApplicationService,
                                       ObjectMapper objectMapper) {
        this.workflowExecutionApplicationService = workflowExecutionApplicationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String invokeDraft(Long workflowId, Map<String, Object> inputs) {
        WorkflowExecutionResultVO result = workflowExecutionApplicationService.debug(
                workflowId,
                new WorkflowExecuteRequest(inputs == null ? Map.of() : inputs));
        if (result.outputs() != null && !result.outputs().isEmpty()) {
            try {
                return objectMapper.writeValueAsString(result.outputs());
            } catch (Exception ex) {
                return String.valueOf(result.outputs());
            }
        }
        if (result.errorMessage() != null && !result.errorMessage().isBlank()) {
            return result.errorMessage();
        }
        return result.status() == null ? "" : result.status();
    }
}
