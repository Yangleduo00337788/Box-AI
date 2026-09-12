package com.boxai.runtime.workflow.executor;

import com.boxai.common.security.SsrfGuard;
import com.boxai.common.security.SsrfSafeHttpClient;
import com.boxai.common.security.WebhookSignature;
import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class WebhookNodeExecutor implements NodeExecutor {

    private final WorkflowTemplateRenderer templateRenderer;

    public WebhookNodeExecutor(WorkflowTemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }

    @Override
    public String nodeType() {
        return "Webhook";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        if (config == null) {
            return NodeExecutionResult.failed("Webhook 节点缺少配置");
        }
        String url = templateRenderer.render(config.path("url").asText(""), context.executionContext().variables());
        if (url.isBlank()) {
            return NodeExecutionResult.failed("Webhook 节点缺少 url");
        }
        String payload = templateRenderer.render(
                config.path("payload").asText("{{input}}"),
                context.executionContext().variables());
        String secret = config.path("secret").asText("");
        String eventType = config.path("eventType").asText("workflow.event");
        long timeoutMs = Math.min(Math.max(config.path("timeoutMs").asLong(10000L), 1000L), 30000L);
        String outputVariable = config.path("outputVariable").asText("webhookResult");
        try {
            Duration timeout = Duration.ofMillis(timeoutMs);
            HttpClient client = SsrfSafeHttpClient.create(timeout, false);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(SsrfGuard.validateHttpUrl(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("X-Box-Event", eventType)
                    .POST(HttpRequest.BodyPublishers.ofString(payload));
            if (secret != null && !secret.isBlank()) {
                builder.header("X-Box-Signature", WebhookSignature.sign(secret, payload));
            }
            HttpResponse<String> response = SsrfSafeHttpClient.send(client, builder.build(), false, timeout);
            Map<String, Object> output = Map.of(
                    "statusCode", response.statusCode(),
                    "body", response.body());
            context.executionContext().setVariable(outputVariable, output);
            return NodeExecutionResult.ok(Map.of(outputVariable, output));
        } catch (Exception ex) {
            return NodeExecutionResult.failed("Webhook 节点执行失败: " + ex.getMessage());
        }
    }
}
