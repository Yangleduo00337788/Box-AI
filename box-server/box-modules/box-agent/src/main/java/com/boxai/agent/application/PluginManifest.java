package com.boxai.agent.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
        JsonNode node = parse(manifestJson, objectMapper);
        if (!(node instanceof ObjectNode objectNode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "manifest 必须是 JSON 对象");
        }
        String key = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        if (!RESOURCE_CATEGORIES.contains(key)) {
            return objectNode.toString();
        }
        if ("mcp".equals(key)) {
            mergeMcpAliases(objectNode);
        }
        if ("tools".equals(key) && text(objectNode, "url", null) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工具插件需填写请求地址");
        }
        if ("mcp".equals(key) && text(objectNode, "endpointUrl", null) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "MCP 插件需配置 endpointUrl 或 mcpServers JSON");
        }
        if ("skills".equals(key)) {
            validateSkill(objectNode);
        }
        if ("knowledge".equals(key)) {
            validateKnowledgeBundle(objectNode);
        }
        if ("workflows".equals(key)) {
            resolveWorkflowDefinition(objectNode, objectMapper, "{\"nodes\":[],\"edges\":[],\"variables\":[]}");
        }
        return objectNode.toString();
    }

    static void mergeMcpAliases(ObjectNode objectNode) {
        if (text(objectNode, "endpointUrl", null) != null) {
            return;
        }
        JsonNode servers = objectNode.get("mcpServers");
        if (servers == null || !servers.isObject() || servers.isEmpty()) {
            return;
        }
        JsonNode first = servers.elements().next();
        if (first == null || !first.isObject()) {
            return;
        }
        String url = text(first, "url", text(first, "endpointUrl", null));
        if (url == null) {
            return;
        }
        objectNode.put("endpointUrl", url);
        if (!objectNode.has("transportType")) {
            objectNode.put("transportType", text(first, "transport", text(first, "transportType", "HTTP")).toUpperCase(Locale.ROOT));
        }
        if (!objectNode.has("authType")) {
            objectNode.put("authType", text(first, "authType", "NONE"));
        }
        if (!objectNode.has("toolCatalogJson") && first.has("tools")) {
            objectNode.put("toolCatalogJson", first.get("tools").toString());
        }
    }

    private static void validateSkill(ObjectNode node) {
        boolean hasText = text(node, "instructions", null) != null;
        boolean hasMd = text(node, "skillMdStorageKey", null) != null;
        boolean hasZip = text(node, "skillPackageStorageKey", null) != null;
        if (!hasText && !hasMd && !hasZip) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Skill 需填写说明、上传 SKILL.md 或上传 Skill 压缩包");
        }
    }

    private static void validateKnowledgeBundle(ObjectNode node) {
        JsonNode bundle = node.get("bundleDocuments");
        if (bundle == null || !bundle.isArray() || bundle.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识库插件需至少上传 1 个文档到资源包");
        }
        for (JsonNode item : bundle) {
            if (item == null || !item.isObject()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "bundleDocuments 项格式错误");
            }
            if (text(item, "storageKey", null) == null || text(item, "fileName", null) == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "bundleDocuments 需包含 storageKey 与 fileName");
            }
        }
    }

    static String resolveSkillInstructions(JsonNode manifest, PluginCatalogAssetApplicationService assets) {
        String inline = text(manifest, "instructions", null);
        if (inline != null && !inline.isBlank()) {
            return inline.trim();
        }
        String mdKey = text(manifest, "skillMdStorageKey", null);
        String zipKey = text(manifest, "skillPackageStorageKey", null);
        if (assets == null) {
            return null;
        }
        return assets.resolveSkillInstructions(mdKey, zipKey);
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

    static ArrayNode bundleDocuments(JsonNode manifest) {
        JsonNode bundle = manifest.get("bundleDocuments");
        if (bundle instanceof ArrayNode arrayNode) {
            return arrayNode;
        }
        return null;
    }
}
