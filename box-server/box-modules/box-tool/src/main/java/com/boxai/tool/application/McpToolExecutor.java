package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.mcp.McpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class McpToolExecutor {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public McpToolExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String execute(McpServer server, String toolName) {
        if (server == null || server.getEndpointUrl() == null || server.getEndpointUrl().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "MCP Server 未配置端点");
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("tool", toolName);
            payload.put("arguments", Map.of());
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(server.getEndpointUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            applyAuth(builder, server);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        ErrorCode.EXECUTION_FAILED,
                        "MCP 工具调用失败: HTTP " + response.statusCode());
            }
            String result = response.body();
            return result == null ? "" : result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "MCP 工具调用失败: " + e.getMessage());
        }
    }

    private void applyAuth(HttpRequest.Builder builder, McpServer server) {
        String authType = server.getAuthType() == null ? "NONE" : server.getAuthType().trim().toUpperCase();
        if ("BEARER".equals(authType) && server.getAuthConfigJson() != null && !server.getAuthConfigJson().isBlank()) {
            try {
                Map<String, Object> config = objectMapper.readValue(server.getAuthConfigJson(), Map.class);
                Object token = config.get("token");
                if (token != null && !String.valueOf(token).isBlank()) {
                    builder.header("Authorization", "Bearer " + String.valueOf(token).trim());
                }
            } catch (Exception ignored) {
                // ignore malformed auth config
            }
        }
    }
}
