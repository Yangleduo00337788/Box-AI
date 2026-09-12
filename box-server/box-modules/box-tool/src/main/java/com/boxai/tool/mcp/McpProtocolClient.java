package com.boxai.tool.mcp;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.SsrfGuard;
import com.boxai.domain.mcp.McpServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class McpProtocolClient {

    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public McpProtocolClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<McpToolDescriptor> listTools(McpServer server) {
        McpSession session = openSession(server);
        try {
            return listTools(session);
        } finally {
            session.close();
        }
    }

    public String callTool(McpServer server, String toolName, Map<String, Object> arguments) {
        McpSession session = openSession(server);
        try {
            return callTool(session, toolName, arguments == null ? Map.of() : arguments);
        } finally {
            session.close();
        }
    }

    private List<McpToolDescriptor> listTools(McpSession session) {
        List<McpToolDescriptor> tools = new ArrayList<>();
        String cursor = null;
        do {
            ObjectNode params = objectMapper.createObjectNode();
            if (cursor != null) {
                params.put("cursor", cursor);
            }
            JsonNode result = session.request("tools/list", params);
            JsonNode toolNodes = result.path("tools");
            if (toolNodes.isArray()) {
                for (JsonNode tool : toolNodes) {
                    String name = text(tool, "name");
                    if (name == null || name.isBlank()) {
                        continue;
                    }
                    tools.add(new McpToolDescriptor(name, text(tool, "description")));
                }
            }
            cursor = text(result, "nextCursor");
        } while (cursor != null && !cursor.isBlank());
        return tools;
    }

    private String callTool(McpSession session, String toolName, Map<String, Object> arguments) {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", objectMapper.valueToTree(arguments));
        JsonNode result = session.request("tools/call", params);
        if (result.path("isError").asBoolean(false)) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, extractContent(result));
        }
        return extractContent(result);
    }

    private McpSession openSession(McpServer server) {
        String transport = server.getTransportType() == null ? "SSE" : server.getTransportType().trim().toUpperCase();
        if ("HTTP".equals(transport) || "STREAMABLE_HTTP".equals(transport) || "STREAMABLE-HTTP".equals(transport)) {
            return McpSession.openStreamableHttp(this, server);
        }
        McpSession streamable = McpSession.tryOpenStreamableHttp(this, server);
        if (streamable != null) {
            return streamable;
        }
        return McpSession.openLegacySse(this, server);
    }

    JsonNode postRpc(McpServer server,
                     URI messageUri,
                     String sessionId,
                     String method,
                     JsonNode params,
                     int requestId,
                     boolean notification) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("jsonrpc", "2.0");
            if (!notification) {
                payload.put("id", requestId);
            }
            payload.put("method", method);
            if (params != null && !params.isEmpty()) {
                payload.set("params", params);
            }
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(messageUri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            applyAuth(builder, server);
            if (sessionId != null && !sessionId.isBlank()) {
                builder.header("Mcp-Session-Id", sessionId);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String sessionHeader = response.headers().firstValue("Mcp-Session-Id").orElse(sessionId);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.EXECUTION_FAILED,
                        "MCP 请求失败: HTTP " + response.statusCode());
            }
            if (notification) {
                ObjectNode ack = objectMapper.createObjectNode();
                if (sessionHeader != null && !sessionHeader.isBlank()) {
                    ack.put("_mcpSessionId", sessionHeader);
                }
                return ack;
            }
            JsonNode message = parseJsonRpcResponse(response.body(), requestId);
            if (message.has("error")) {
                JsonNode error = message.get("error");
                String msg = error.path("message").asText("MCP 调用失败");
                throw new BusinessException(ErrorCode.EXECUTION_FAILED, msg);
            }
            if (sessionHeader != null && !sessionHeader.isBlank()) {
                // allow caller to pick up session id from thread-local style return via node
                if (message.isObject()) {
                    ((ObjectNode) message).put("_mcpSessionId", sessionHeader);
                }
            }
            return message;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "MCP 请求失败: " + ex.getMessage());
        }
    }

    String discoverLegacyMessagePath(McpServer server, URI sseUri) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(sseUri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "text/event-stream")
                    .GET();
            applyAuth(builder, server);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            return parseSseEndpointEvent(response.body());
        } catch (Exception ex) {
            return null;
        }
    }

    private String parseSseEndpointEvent(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        String[] lines = body.split("\r?\n");
        String event = null;
        for (String line : lines) {
            if (line.startsWith("event:")) {
                event = line.substring("event:".length()).trim();
            } else if (line.startsWith("data:") && "endpoint".equalsIgnoreCase(event)) {
                return line.substring("data:".length()).trim();
            }
        }
        return null;
    }

    JsonNode parseJsonRpcResponse(String body, int requestId) throws Exception {
        if (body == null || body.isBlank()) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "MCP 响应为空");
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            JsonNode root = objectMapper.readTree(trimmed);
            if (root.isArray()) {
                for (JsonNode item : root) {
                    if (matchesRequest(item, requestId)) {
                        return item;
                    }
                }
            }
            return root;
        }
        for (String line : trimmed.split("\r?\n")) {
            if (!line.startsWith("data:")) {
                continue;
            }
            String data = line.substring("data:".length()).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) {
                continue;
            }
            JsonNode node = objectMapper.readTree(data);
            if (matchesRequest(node, requestId)) {
                return node;
            }
        }
        throw new BusinessException(ErrorCode.EXECUTION_FAILED, "未找到匹配的 MCP JSON-RPC 响应");
    }

    private boolean matchesRequest(JsonNode node, int requestId) {
        if (node == null || !node.has("id")) {
            return false;
        }
        JsonNode idNode = node.get("id");
        if (idNode.isInt()) {
            return idNode.asInt() == requestId;
        }
        return String.valueOf(requestId).equals(idNode.asText());
    }

    URI resolveMessageUri(URI baseUri, String messagePath) {
        if (messagePath == null || messagePath.isBlank()) {
            return baseUri;
        }
        if (messagePath.startsWith("http://") || messagePath.startsWith("https://")) {
            return SsrfGuard.validateHttpUrl(messagePath);
        }
        String base = baseUri.getScheme() + "://" + baseUri.getAuthority();
        if (!messagePath.startsWith("/")) {
            String path = baseUri.getPath();
            if (path.endsWith("/")) {
                return SsrfGuard.validateHttpUrl(base + path + messagePath);
            }
            int idx = path.lastIndexOf('/');
            String prefix = idx >= 0 ? path.substring(0, idx + 1) : "/";
            return SsrfGuard.validateHttpUrl(base + prefix + messagePath);
        }
        return SsrfGuard.validateHttpUrl(base + messagePath);
    }

    ObjectNode initializeParams() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("protocolVersion", PROTOCOL_VERSION);
        params.set("capabilities", objectMapper.createObjectNode());
        ObjectNode clientInfo = objectMapper.createObjectNode();
        clientInfo.put("name", "box-ai");
        clientInfo.put("version", "1.0.0");
        params.set("clientInfo", clientInfo);
        return params;
    }

    void applyAuth(HttpRequest.Builder builder, McpServer server) {
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

    public String catalogJson(List<McpToolDescriptor> tools) {
        ArrayNode array = objectMapper.createArrayNode();
        for (McpToolDescriptor tool : tools) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("name", tool.name());
            item.put("description", tool.description() == null ? tool.name() : tool.description());
            array.add(item);
        }
        try {
            return objectMapper.writeValueAsString(array);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private String extractContent(JsonNode result) {
        JsonNode content = result.path("content");
        if (!content.isArray() || content.isEmpty()) {
            return result.toString();
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode item : content) {
            if ("text".equals(item.path("type").asText())) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(item.path("text").asText(""));
            }
        }
        return builder.length() == 0 ? result.toString() : builder.toString();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    static final class McpSession {
        private final McpProtocolClient client;
        private final McpServer server;
        private final URI messageUri;
        private String sessionId;
        private final AtomicInteger requestId = new AtomicInteger(1);
        private boolean initialized;

        private McpSession(McpProtocolClient client, McpServer server, URI messageUri, String sessionId) {
            this.client = client;
            this.server = server;
            this.messageUri = messageUri;
            this.sessionId = sessionId;
        }

        static McpSession openStreamableHttp(McpProtocolClient client, McpServer server) {
            McpSession session = tryOpenStreamableHttp(client, server);
            if (session == null) {
                throw new BusinessException(ErrorCode.EXECUTION_FAILED, "无法建立 MCP Streamable HTTP 会话");
            }
            return session;
        }

        static McpSession tryOpenStreamableHttp(McpProtocolClient client, McpServer server) {
            URI endpoint = SsrfGuard.validateHttpUrl(server.getEndpointUrl());
            McpSession session = new McpSession(client, server, endpoint, null);
            if (!session.handshake()) {
                return null;
            }
            return session;
        }

        static McpSession openLegacySse(McpProtocolClient client, McpServer server) {
            URI endpoint = SsrfGuard.validateHttpUrl(server.getEndpointUrl());
            String messagePath = client.discoverLegacyMessagePath(server, endpoint);
            URI messageUri = messagePath == null ? endpoint : client.resolveMessageUri(endpoint, messagePath);
            McpSession session = new McpSession(client, server, messageUri, null);
            if (!session.handshake()) {
                throw new BusinessException(ErrorCode.EXECUTION_FAILED, "无法建立 MCP SSE 会话");
            }
            return session;
        }

        JsonNode request(String method, JsonNode params) {
            int id = requestId.getAndIncrement();
            JsonNode response = client.postRpc(server, messageUri, sessionId, method, params, id, false);
            if (response.has("_mcpSessionId")) {
                sessionId = response.get("_mcpSessionId").asText();
            }
            return response.path("result");
        }

        private boolean handshake() {
            if (initialized) {
                return true;
            }
            int initId = requestId.getAndIncrement();
            JsonNode initResponse = client.postRpc(
                    server, messageUri, sessionId, "initialize", client.initializeParams(), initId, false);
            if (initResponse.has("_mcpSessionId")) {
                sessionId = initResponse.get("_mcpSessionId").asText();
            }
            if (initResponse.has("error")) {
                return false;
            }
            client.postRpc(server, messageUri, sessionId, "notifications/initialized", null, 0, true);
            initialized = true;
            return true;
        }

        void close() {
            // Streamable HTTP sessions are short-lived per sync/call in this implementation.
        }
    }

    public record McpToolDescriptor(String name, String description) {
    }
}
