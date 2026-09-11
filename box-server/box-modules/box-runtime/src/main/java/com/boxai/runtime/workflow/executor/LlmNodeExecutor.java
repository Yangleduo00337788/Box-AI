package com.boxai.runtime.workflow.executor;

import com.boxai.ai.ChatModelGateway;
import com.boxai.common.exception.BusinessException;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.model.platform.ResolvedPlatformModel;
import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LlmNodeExecutor implements NodeExecutor {

    private final WorkflowTemplateRenderer templateRenderer;
    private final PlatformModelApplicationService platformModelApplicationService;
    private final ChatModelGateway chatModelGateway;

    public LlmNodeExecutor(WorkflowTemplateRenderer templateRenderer,
                           PlatformModelApplicationService platformModelApplicationService,
                           ChatModelGateway chatModelGateway) {
        this.templateRenderer = templateRenderer;
        this.platformModelApplicationService = platformModelApplicationService;
        this.chatModelGateway = chatModelGateway;
    }

    @Override
    public String nodeType() {
        return "LLM";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        if (config == null || !config.has("platformModelId")) {
            return NodeExecutionResult.failed("LLM 节点缺少 platformModelId");
        }
        long platformModelId = config.get("platformModelId").asLong();
        String systemPrompt = templateRenderer.render(
                config.path("systemPrompt").asText(""),
                context.executionContext().variables());
        String userPrompt = templateRenderer.render(
                config.path("userPrompt").asText(config.path("prompt").asText("")),
                context.executionContext().variables());
        if (userPrompt.isBlank()) {
            return NodeExecutionResult.failed("LLM 节点缺少 prompt");
        }
        String outputVariable = config.path("outputVariable").asText("llmResult");
        double temperature = config.path("temperature").asDouble(0.7D);
        double topP = config.path("topP").asDouble(1.0D);
        int maxTokens = config.path("maxTokens").asInt(4096);
        try {
            ResolvedPlatformModel resolved = platformModelApplicationService.resolveForChat(platformModelId);
            String content = chatModelGateway.chat(
                    resolved.runtimeConfig(),
                    systemPrompt.isBlank() ? null : systemPrompt,
                    userPrompt,
                    temperature,
                    topP,
                    maxTokens);
            context.executionContext().setVariable(outputVariable, content);
            return NodeExecutionResult.ok(Map.of(outputVariable, content));
        } catch (BusinessException ex) {
            return NodeExecutionResult.failed(ex.getMessage());
        } catch (Exception ex) {
            return NodeExecutionResult.failed("LLM 节点执行失败: " + ex.getMessage());
        }
    }
}
