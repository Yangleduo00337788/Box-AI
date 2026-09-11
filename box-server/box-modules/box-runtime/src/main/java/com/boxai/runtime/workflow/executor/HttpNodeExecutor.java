package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class HttpNodeExecutor implements NodeExecutor {

    private final WorkflowTemplateRenderer templateRenderer;

    public HttpNodeExecutor(WorkflowTemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }

    @Override
    public String nodeType() {
        return "HTTP";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        if (config == null) {
            return NodeExecutionResult.failed("HTTP 节点缺少配置");
        }
        String url = templateRenderer.render(config.path("url").asText(""), context.executionContext().variables());
        if (url.isBlank()) {
            return NodeExecutionResult.failed("HTTP 节点缺少 url");
        }
        String method = config.path("method").asText("GET").toUpperCase();
        String body = templateRenderer.render(config.path("body").asText(""), context.executionContext().variables());
        long timeoutMs = Math.min(Math.max(config.path("timeoutMs").asLong(10000L), 1000L), 30000L);
        String outputVariable = config.path("outputVariable").asText("httpResult");
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .build();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs));
            if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, Object> output = Map.of(
                    "statusCode", response.statusCode(),
                    "body", response.body());
            context.executionContext().setVariable(outputVariable, output);
            return NodeExecutionResult.ok(Map.of(outputVariable, output));
        } catch (Exception ex) {
            return NodeExecutionResult.failed("HTTP 节点执行失败: " + ex.getMessage());
        }
    }
}
