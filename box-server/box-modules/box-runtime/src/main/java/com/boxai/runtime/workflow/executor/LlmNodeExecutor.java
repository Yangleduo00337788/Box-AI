package com.boxai.runtime.workflow.executor;

import com.boxai.ai.ChatModelGateway;
import com.boxai.ai.ChatStreamHandler;
import com.boxai.common.exception.BusinessException;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.model.platform.ResolvedPlatformModel;
import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.core.WorkflowStreamCallback;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.tenant.application.QuotaApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LlmNodeExecutor implements NodeExecutor {

    private final WorkflowTemplateRenderer templateRenderer;
    private final PlatformModelApplicationService platformModelApplicationService;
    private final ChatModelGateway chatModelGateway;
    private final QuotaApplicationService quotaApplicationService;

    public LlmNodeExecutor(WorkflowTemplateRenderer templateRenderer,
                           PlatformModelApplicationService platformModelApplicationService,
                           ChatModelGateway chatModelGateway,
                           QuotaApplicationService quotaApplicationService) {
        this.templateRenderer = templateRenderer;
        this.platformModelApplicationService = platformModelApplicationService;
        this.chatModelGateway = chatModelGateway;
        this.quotaApplicationService = quotaApplicationService;
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
            Long workspaceId = WorkspaceContext.require().workspaceId();
            quotaApplicationService.assertAiQuotaAvailable(workspaceId);
            ResolvedPlatformModel resolved = platformModelApplicationService.resolveForChat(platformModelId);
            String content;
            boolean stream = shouldStream(context, config);
            if (stream) {
                StringBuilder builder = new StringBuilder();
                WorkflowStreamCallback callback = context.executionContext().streamCallback();
                String nodeId = context.node().id();
                String nodeType = context.node().type();
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Throwable> streamError = new AtomicReference<>();
                chatModelGateway.streamChat(
                        resolved.runtimeConfig(),
                        systemPrompt.isBlank() ? null : systemPrompt,
                        userPrompt,
                        temperature,
                        topP,
                        maxTokens,
                        new ChatStreamHandler() {
                            @Override
                            public void onPartial(String partial) {
                                builder.append(partial);
                                if (callback != null) {
                                    callback.onDelta(nodeId, nodeType, partial);
                                }
                            }

                            @Override
                            public void onComplete() {
                                latch.countDown();
                            }

                            @Override
                            public void onError(Throwable error) {
                                streamError.set(error);
                                latch.countDown();
                            }
                        });
                if (!latch.await(120, TimeUnit.SECONDS)) {
                    return NodeExecutionResult.failed("LLM 节点流式生成超时");
                }
                if (streamError.get() != null) {
                    return NodeExecutionResult.failed("LLM 节点流式生成失败: " + streamError.get().getMessage());
                }
                content = builder.toString();
            } else {
                content = chatModelGateway.chat(
                        resolved.runtimeConfig(),
                        systemPrompt.isBlank() ? null : systemPrompt,
                        userPrompt,
                        temperature,
                        topP,
                        maxTokens);
            }
            quotaApplicationService.consumeAiUsage(workspaceId, estimateTokens(systemPrompt, userPrompt, content));
            context.executionContext().setVariable(outputVariable, content);
            return NodeExecutionResult.ok(Map.of(outputVariable, content));
        } catch (BusinessException ex) {
            return NodeExecutionResult.failed(ex.getMessage());
        } catch (Exception ex) {
            return NodeExecutionResult.failed("LLM 节点执行失败: " + ex.getMessage());
        }
    }

    private boolean shouldStream(NodeExecutionContext context, JsonNode config) {
        if (context.executionContext().streamCallback() != null) {
            return true;
        }
        return context.executionContext().debugMode() && config.path("streamEnabled").asBoolean(true);
    }

    private long estimateTokens(String systemPrompt, String userPrompt, String response) {
        long total = response == null ? 0 : response.length();
        if (systemPrompt != null) {
            total += systemPrompt.length();
        }
        if (userPrompt != null) {
            total += userPrompt.length();
        }
        return Math.max(total, 1L);
    }
}
