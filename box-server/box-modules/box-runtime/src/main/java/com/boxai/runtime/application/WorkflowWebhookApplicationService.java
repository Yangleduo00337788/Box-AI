package com.boxai.runtime.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.WebhookSignature;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.runtime.api.WorkflowExecuteRequest;
import com.boxai.runtime.api.WorkflowExecutionResultVO;
import com.boxai.security.context.WorkspaceContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WorkflowWebhookApplicationService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionApplicationService workflowExecutionApplicationService;
    private final ObjectMapper objectMapper;

    public WorkflowWebhookApplicationService(WorkflowRepository workflowRepository,
                                             WorkflowExecutionApplicationService workflowExecutionApplicationService,
                                             ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.workflowExecutionApplicationService = workflowExecutionApplicationService;
        this.objectMapper = objectMapper;
    }

    public WorkflowExecutionResultVO trigger(String token, String payload, String signature) {
        Workflow workflow = workflowRepository.findByWebhookToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Webhook 不存在或已失效"));
        if (workflow.getPublishedVersionId() == null || !"PUBLISHED".equalsIgnoreCase(workflow.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流尚未发布");
        }
        if (!WebhookSignature.verify(workflow.getWebhookSecret(), payload, signature)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Webhook 签名校验失败");
        }
        Map<String, Object> inputs = parsePayload(payload);
        try {
            WorkspaceContext.set(new WorkspaceContext(
                    workflow.getWorkspaceId(),
                    workflow.getCreatedBy(),
                    null,
                    "WEBHOOK"));
            return workflowExecutionApplicationService.executeWebhook(workflow.getId(), new WorkflowExecuteRequest(inputs));
        } finally {
            WorkspaceContext.clear();
        }
    }

    private Map<String, Object> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(payload, new TypeReference<>() {
            });
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("input", parsed);
            return inputs;
        } catch (Exception ex) {
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("input", Map.of("message", payload));
            return inputs;
        }
    }
}
