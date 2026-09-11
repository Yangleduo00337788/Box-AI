package com.boxai.infrastructure.elasticsearch;

import com.boxai.domain.knowledge.KnowledgeChunk;
import com.boxai.domain.knowledge.KnowledgeChunkSearchIndex;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ElasticsearchKnowledgeChunkSearchIndex implements KnowledgeChunkSearchIndex {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchKnowledgeChunkSearchIndex.class);
    private static final String INDEX = "box-knowledge-chunks";
    private static final int VECTOR_DIMS = 1536;

    private final ElasticsearchProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public ElasticsearchKnowledgeChunkSearchIndex(ElasticsearchProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void ensureIndex() {
        try {
            HttpResponse<String> head = send("HEAD", "/" + INDEX, null);
            if (head.statusCode() == 200) {
                return;
            }
            ObjectNode mappings = objectMapper.createObjectNode();
            ObjectNode properties = objectMapper.createObjectNode();
            properties.set("chunk_id", objectMapper.createObjectNode().put("type", "long"));
            properties.set("knowledge_base_id", objectMapper.createObjectNode().put("type", "long"));
            properties.set("workspace_id", objectMapper.createObjectNode().put("type", "long"));
            properties.set("document_id", objectMapper.createObjectNode().put("type", "long"));
            properties.set("content", objectMapper.createObjectNode().put("type", "text"));
            ObjectNode vector = objectMapper.createObjectNode();
            vector.put("type", "dense_vector");
            vector.put("dims", VECTOR_DIMS);
            vector.put("index", true);
            vector.put("similarity", "cosine");
            properties.set("embedding", vector);
            mappings.set("properties", properties);
            ObjectNode body = objectMapper.createObjectNode();
            body.set("mappings", mappings);
            send("PUT", "/" + INDEX, body.toString());
        } catch (Exception e) {
            log.warn("Elasticsearch index init skipped: {}", e.getMessage());
        }
    }

    @Override
    public void indexChunk(KnowledgeChunk chunk, float[] embedding) {
        if (chunk.getEsDocumentId() == null || embedding == null || embedding.length == 0) {
            return;
        }
        try {
            ObjectNode doc = objectMapper.createObjectNode();
            doc.put("chunk_id", chunk.getId());
            doc.put("knowledge_base_id", chunk.getKnowledgeBaseId());
            doc.put("workspace_id", chunk.getWorkspaceId());
            doc.put("document_id", chunk.getDocumentId());
            doc.put("content", chunk.getContent());
            ArrayNode vector = objectMapper.createArrayNode();
            for (float value : embedding) {
                vector.add(value);
            }
            doc.set("embedding", vector);
            send("PUT", "/" + INDEX + "/_doc/" + chunk.getEsDocumentId(), doc.toString());
        } catch (Exception e) {
            log.warn("Failed to index chunk {}: {}", chunk.getId(), e.getMessage());
        }
    }

    @Override
    public void deleteByDocument(Long documentId) {
        try {
            ObjectNode query = objectMapper.createObjectNode();
            ObjectNode term = objectMapper.createObjectNode();
            term.put("document_id", documentId);
            query.set("term", term);
            ObjectNode body = objectMapper.createObjectNode();
            body.set("query", query);
            send("POST", "/" + INDEX + "/_delete_by_query", body.toString());
        } catch (Exception e) {
            log.warn("Failed to delete chunks for document {}: {}", documentId, e.getMessage());
        }
    }

    @Override
    public void deleteByKnowledgeBase(Long knowledgeBaseId) {
        try {
            ObjectNode query = objectMapper.createObjectNode();
            ObjectNode term = objectMapper.createObjectNode();
            term.put("knowledge_base_id", knowledgeBaseId);
            query.set("term", term);
            ObjectNode body = objectMapper.createObjectNode();
            body.set("query", query);
            send("POST", "/" + INDEX + "/_delete_by_query", body.toString());
        } catch (Exception e) {
            log.warn("Failed to delete chunks for knowledge base {}: {}", knowledgeBaseId, e.getMessage());
        }
    }

    @Override
    public List<Long> searchByVector(Long knowledgeBaseId, float[] queryEmbedding, int topK) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return List.of();
        }
        try {
            ObjectNode knn = objectMapper.createObjectNode();
            knn.put("field", "embedding");
            ArrayNode queryVector = objectMapper.createArrayNode();
            for (float value : queryEmbedding) {
                queryVector.add(value);
            }
            knn.set("query_vector", queryVector);
            knn.put("k", Math.max(topK, 1));
            knn.put("num_candidates", Math.max(topK * 10, 20));
            ObjectNode filter = objectMapper.createObjectNode();
            ObjectNode term = objectMapper.createObjectNode();
            term.put("knowledge_base_id", knowledgeBaseId);
            filter.set("term", term);
            knn.set("filter", filter);
            ObjectNode body = objectMapper.createObjectNode();
            body.put("size", Math.max(topK, 1));
            body.set("knn", knn);
            HttpResponse<String> response = send("POST", "/" + INDEX + "/_search", body.toString());
            return parseChunkIds(response.body());
        } catch (Exception e) {
            log.warn("Vector search failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Long> searchByKeyword(Long knowledgeBaseId, String keyword, int topK) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        try {
            ObjectNode must = objectMapper.createObjectNode();
            ObjectNode match = objectMapper.createObjectNode();
            match.put("query", keyword);
            must.set("match", objectMapper.createObjectNode().set("content", match));
            ObjectNode filter = objectMapper.createObjectNode();
            ObjectNode term = objectMapper.createObjectNode();
            term.put("knowledge_base_id", knowledgeBaseId);
            filter.set("term", term);
            ObjectNode bool = objectMapper.createObjectNode();
            bool.set("must", objectMapper.createArrayNode().add(must));
            bool.set("filter", objectMapper.createArrayNode().add(filter));
            ObjectNode query = objectMapper.createObjectNode();
            query.set("bool", bool);
            ObjectNode body = objectMapper.createObjectNode();
            body.put("size", Math.max(topK, 1));
            body.set("query", query);
            HttpResponse<String> response = send("POST", "/" + INDEX + "/_search", body.toString());
            return parseChunkIds(response.body());
        } catch (Exception e) {
            log.warn("Keyword ES search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Long> parseChunkIds(String body) throws Exception {
        List<Long> ids = new ArrayList<>();
        var root = objectMapper.readTree(body);
        var hits = root.path("hits").path("hits");
        if (!hits.isArray()) {
            return ids;
        }
        for (var hit : hits) {
            long chunkId = hit.path("_source").path("chunk_id").asLong(0L);
            if (chunkId > 0) {
                ids.add(chunkId);
            }
        }
        return ids;
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json");
        if ("HEAD".equals(method)) {
            builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
        } else if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://" + properties.getHost() + ":" + properties.getPort();
    }
}
