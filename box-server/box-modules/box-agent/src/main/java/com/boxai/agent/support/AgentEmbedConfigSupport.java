package com.boxai.agent.support;

import com.boxai.agent.api.AgentEmbedConfigVO;
import com.boxai.agent.api.UpdateAgentEmbedConfigRequest;
import com.boxai.common.security.EmbedDomainNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AgentEmbedConfigSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_THEME = "#0052d9";

    private AgentEmbedConfigSupport() {
    }

    public static AgentEmbedConfigVO toVO(String configJson, String agentName) {
        ObjectNode root = readRoot(configJson);
        JsonNode embed = root.get("embed");
        if (embed == null || embed.isNull()) {
            return new AgentEmbedConfigVO(DEFAULT_THEME, "", "", List.of(), agentName, "", false, null, false);
        }
        return new AgentEmbedConfigVO(
                textOrDefault(embed.get("themeColor"), DEFAULT_THEME),
                textOrDefault(embed.get("logoUrl"), ""),
                textOrDefault(embed.get("welcomeMessage"), ""),
                readQuestions(embed.get("suggestedQuestions")),
                agentName,
                textOrDefault(embed.get("customDomain"), ""),
                embed.path("domainVerified").asBoolean(false),
                null,
                false);
    }

    public static AgentEmbedConfigVO withDomain(AgentEmbedConfigVO vo,
                                                String customDomain,
                                                boolean verified,
                                                String verifyToken,
                                                boolean verifySkipped) {
        if (vo == null) {
            return new AgentEmbedConfigVO(
                    DEFAULT_THEME, "", "", List.of(), null, customDomain, verified, verifyToken, verifySkipped);
        }
        return new AgentEmbedConfigVO(
                vo.themeColor(),
                vo.logoUrl(),
                vo.welcomeMessage(),
                vo.suggestedQuestions(),
                vo.agentName(),
                customDomain,
                verified,
                verifyToken,
                verifySkipped);
    }

    public static String merge(String configJson, UpdateAgentEmbedConfigRequest request) {
        ObjectNode root = readRoot(configJson);
        ObjectNode embed = MAPPER.createObjectNode();
        embed.put("themeColor", normalizeTheme(request.themeColor()));
        embed.put("logoUrl", normalizeText(request.logoUrl()));
        embed.put("welcomeMessage", normalizeText(request.welcomeMessage()));
        ArrayNode questions = embed.putArray("suggestedQuestions");
        for (String question : normalizeQuestions(request.suggestedQuestions())) {
            questions.add(question);
        }
        String domain = EmbedDomainNormalizer.normalize(request.customDomain());
        embed.put("customDomain", domain);
        root.set("embed", embed);
        return root.toString();
    }

    private static ObjectNode readRoot(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return MAPPER.createObjectNode();
        }
        try {
            JsonNode node = MAPPER.readTree(configJson);
            if (node instanceof ObjectNode objectNode) {
                return objectNode.deepCopy();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return MAPPER.createObjectNode();
    }

    private static List<String> readQuestions(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> questions = new ArrayList<>();
        Iterator<JsonNode> iterator = node.elements();
        while (iterator.hasNext()) {
            String value = iterator.next().asText("").trim();
            if (!value.isEmpty()) {
                questions.add(value);
            }
        }
        return List.copyOf(questions);
    }

    private static List<String> normalizeQuestions(List<String> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String question : questions) {
            if (question == null) {
                continue;
            }
            String value = question.trim();
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }

    private static String normalizeTheme(String themeColor) {
        String value = normalizeText(themeColor);
        return value.isEmpty() ? DEFAULT_THEME : value;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String textOrDefault(JsonNode node, String defaultValue) {
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        String value = node.asText("").trim();
        return value.isEmpty() ? defaultValue : value;
    }
}
