package com.boxai.agent.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Locale;
import java.util.Set;

final class PluginManifest {

    private static final Set<String> RESOURCE_CATEGORIES = Set.of("tools", "workflows", "knowledge", "mcp", "skills");

    private PluginManifest() {
    }

    static JsonNode parse(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return node == null || node.isNull() ? objectMapper.createObjectNode() : node;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "插件资源配置不是合法 JSON");
        }
    }

    static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    static int intValue(JsonNode node, String field, int fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asInt(fallback);
    }

    static boolean boolValue(JsonNode node, String field, boolean fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asBoolean(fallback);
    }

    static String normalize(String json, ObjectMapper objectMapper) {
        JsonNode node = parse(json, objectMapper);
        if (node instanceof ObjectNode objectNode) {
            return objectNode.toString();
        }
        return node.toString();
    }

    static String normalizeAndValidate(String category, String manifestJson, ObjectMapper objectMapper) {
        String normalized = normalize(manifestJson, objectMapper);
        JsonNode node = parse(normalized, objectMapper);
        String key = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        if (!RESOURCE_CATEGORIES.contains(key)) {
            return normalized;
        }
        if ("tools".equals(key) && text(node, "url", null) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工具插件需填写请求地址");
        }
        if ("mcp".equals(key) && text(node, "endpointUrl", null) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "MCP 插件需填写服务地址");
        }
        if ("skills".equals(key) && text(node, "instructions", null) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Skill 插件需填写技能说明");
        }
        if ("workflows".equals(key)) {
            resolveWorkflowDefinition(node, objectMapper, "{\"nodes\":[],\"edges\":[],\"variables\":[]}");
        }
        return normalized;
    }

    static String resolveWorkflowDefinition(JsonNode manifest, ObjectMapper objectMapper, String defaultDefinition) {
        JsonNode value = manifest.get("definitionJson");
        if (value == null || value.isNull()) {
            value = manifest.get("definition");
        }
        if (value == null || value.isNull()) {
            return defaultDefinition;
        }
        String raw;
        if (value.isTextual()) {
            raw = value.asText().trim();
        } else if (value.isObject() || value.isArray()) {
            raw = value.toString();
        } else {
            return defaultDefinition;
        }
        if (raw.isBlank()) {
            return defaultDefinition;
        }
        try {
            JsonNode parsed = objectMapper.readTree(raw);
            if (!parsed.isObject()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流定义必须是 JSON 对象");
            }
            return raw;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流定义不是合法的 JSON，请检查插件配置");
        }
    }
}
