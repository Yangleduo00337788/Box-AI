package com.boxai.infrastructure.ai;

import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.ai.RerankModelGateway;
import com.boxai.ai.RerankScore;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.SsrfGuard;
import com.boxai.common.security.SsrfSafeHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class OpenAiCompatibleRerankGateway implements RerankModelGateway {

    private final ObjectMapper objectMapper;

    public OpenAiCompatibleRerankGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RerankScore> rerank(ModelRuntimeConfig config,
                                    String query,
                                    List<String> documents,
                                    int topN) {
        if (query == null || query.isBlank() || documents == null || documents.isEmpty()) {
            return List.of();
        }
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Rerank API Key 未配置");
        }
        String modelName = config.modelName() == null || config.modelName().isBlank()
                ? "rerank-english-v3.0"
                : config.modelName();
        int safeTopN = topN < 1 ? documents.size() : Math.min(topN, documents.size());

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", modelName);
        payload.put("query", query.trim());
        payload.put("top_n", safeTopN);
        ArrayNode docs = payload.putArray("documents");
        for (String document : documents) {
            docs.add(document == null ? "" : document);
        }

        String endpoint = normalizeRerankUrl(config.baseUrl());
        URI endpointUri = SsrfGuard.validateHttpUrl(endpoint);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpointUri)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();
        try {
            var client = SsrfSafeHttpClient.create(Duration.ofSeconds(10), false);
            HttpResponse<String> response = SsrfSafeHttpClient.send(
                    client, request, false, Duration.ofSeconds(60));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.EXECUTION_FAILED,
                        "Rerank 请求失败: HTTP " + response.statusCode());
            }
            return parseResults(response.body(), safeTopN);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "Rerank 调用失败");
        }
    }

    private List<RerankScore> parseResults(String body, int topN) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode results = root.get("results");
        if (results == null || !results.isArray() || results.isEmpty()) {
            return List.of();
        }
        List<RerankScore> scores = new ArrayList<>();
        for (JsonNode item : results) {
            int index = item.path("index").asInt(-1);
            double relevance = item.has("relevance_score")
                    ? item.get("relevance_score").asDouble()
                    : item.path("score").asDouble(0D);
            if (index >= 0) {
                scores.add(new RerankScore(index, relevance));
            }
        }
        return scores.stream()
                .sorted(Comparator.comparingDouble(RerankScore::score).reversed())
                .limit(topN)
                .toList();
    }

    private String normalizeRerankUrl(String baseUrl) {
        String normalized = baseUrl == null || baseUrl.isBlank()
                ? "https://api.cohere.com/v1"
                : baseUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/rerank")) {
            return normalized;
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/rerank";
        }
        return normalized + "/v1/rerank";
    }
}
