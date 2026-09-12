package com.boxai.runtime.workflow.executor;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.domain.workflow.WorkflowVersion;
import com.boxai.domain.workflow.WorkflowVersionRepository;
import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.core.WorkflowExecutionContext;
import com.boxai.runtime.workflow.core.WorkflowExecutionResult;
import com.boxai.runtime.workflow.engine.DefaultWorkflowExecutor;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.workflow.application.WorkflowDefinitionValidator;
import com.boxai.workflow.api.WorkflowValidateVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SubWorkflowNodeExecutor implements NodeExecutor {

    private static final int MAX_SUB_DEPTH = 5;

    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final DefaultWorkflowExecutor workflowExecutor;
    private final WorkflowDefinitionValidator workflowDefinitionValidator;
    private final WorkflowTemplateRenderer templateRenderer;
    private final ObjectMapper objectMapper;

    public SubWorkflowNodeExecutor(WorkflowRepository workflowRepository,
                                   WorkflowVersionRepository workflowVersionRepository,
                                   @Lazy DefaultWorkflowExecutor workflowExecutor,
                                   WorkflowDefinitionValidator workflowDefinitionValidator,
                                   WorkflowTemplateRenderer templateRenderer,
                                   ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.workflowExecutor = workflowExecutor;
        this.workflowDefinitionValidator = workflowDefinitionValidator;
        this.templateRenderer = templateRenderer;
        this.objectMapper = objectMapper;
    }

    @Override
    public String nodeType() {
        return "SubWorkflow";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        if (config == null || !config.has("workflowId")) {
            return NodeExecutionResult.failed("Sub Workflow 节点缺少 workflowId");
        }
        long childWorkflowId = config.get("workflowId").asLong();
        WorkflowExecutionContext parentContext = context.executionContext();
        if (parentContext.callStack().contains(childWorkflowId)) {
            return NodeExecutionResult.failed("子工作流存在循环引用");
        }
        if (parentContext.depth() >= MAX_SUB_DEPTH) {
            return NodeExecutionResult.failed("子工作流嵌套超过最大层数 " + MAX_SUB_DEPTH);
        }

        Workflow childWorkflow = workflowRepository.findById(childWorkflowId).orElse(null);
        if (childWorkflow == null) {
            return NodeExecutionResult.failed("子工作流不存在");
        }
        if (!WorkspaceContext.require().workspaceId().equals(childWorkflow.getWorkspaceId())) {
            return NodeExecutionResult.failed("无权访问子工作流");
        }

        WorkflowVersion childVersion;
        try {
            childVersion = resolveChildVersion(childWorkflow, parentContext.debugMode());
        } catch (BusinessException ex) {
            return NodeExecutionResult.failed(ex.getMessage());
        }
        WorkflowValidateVO validation = workflowDefinitionValidator.validate(childVersion.getDefinitionJson());
        if (!validation.valid()) {
            return NodeExecutionResult.failed("子工作流定义无效: " + String.join("；", validation.errors()));
        }

        Map<String, Object> childInputs;
        try {
            childInputs = parseInputs(config, parentContext.variables());
        } catch (BusinessException ex) {
            return NodeExecutionResult.failed(ex.getMessage());
        }
        WorkflowExecutionContext childContext = WorkflowExecutionContext.child(
                parentContext,
                childWorkflowId,
                childVersion.getId(),
                childInputs);
        WorkflowExecutionResult childResult = workflowExecutor.execute(childVersion.getDefinitionJson(), childContext);
        if (!"SUCCEEDED".equals(childResult.status())) {
            String message = childResult.errorMessage() == null ? "未知错误" : childResult.errorMessage();
            return NodeExecutionResult.failed("子工作流执行失败: " + message);
        }

        String outputVariable = config.path("outputVariable").asText("subWorkflowResult");
        Map<String, Object> output = Map.of(
                "workflowId", childWorkflowId,
                "versionId", childVersion.getId(),
                "status", childResult.status(),
                "outputs", childResult.outputs());
        parentContext.setVariable(outputVariable, output);
        return NodeExecutionResult.ok(Map.of(outputVariable, output));
    }

    private WorkflowVersion resolveChildVersion(Workflow childWorkflow, boolean debugMode) {
        if (debugMode) {
            return workflowVersionRepository.findLatestDraft(childWorkflow.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_VERSION_NOT_FOUND, "子工作流草稿不存在"));
        }
        if (childWorkflow.getPublishedVersionId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "子工作流尚未发布");
        }
        return workflowVersionRepository.findById(childWorkflow.getPublishedVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_VERSION_NOT_FOUND, "子工作流发布版本不存在"));
    }

    private Map<String, Object> parseInputs(JsonNode config, Map<String, Object> parentVariables) {
        String inputsTemplate = config.path("inputsJson").asText("");
        if (inputsTemplate == null || inputsTemplate.isBlank()) {
            return new LinkedHashMap<>(parentVariables);
        }
        try {
            String rendered = templateRenderer.render(inputsTemplate, parentVariables);
            if (rendered.isBlank()) {
                return new LinkedHashMap<>(parentVariables);
            }
            return objectMapper.readValue(rendered, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "子工作流入参模板解析失败");
        }
    }
}
