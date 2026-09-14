package com.boxai.agent.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class PluginManifest {

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
}
