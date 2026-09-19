package com.boxai.infrastructure.elasticsearch;

import com.boxai.domain.agent.AgentLongTermMemory;
import com.boxai.domain.agent.AgentLongTermMemorySearchIndex;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ElasticsearchAgentLongTermMemorySearchIndex implements AgentLongTermMemorySearchIndex {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchAgentLongTermMemorySearchIndex.class);
    private static final String INDEX = "box-agent-long-term-memory";
    private static final int VECTOR_DIMS = 1536;

    private final ElasticsearchProperties properties;
    private final ElasticsearchHttpClient elasticsearchHttpClient;
    private final ObjectMapper objectMapper;

    public ElasticsearchAgentLongTermMemorySearchIndex(ElasticsearchProperties properties,
                                                       ElasticsearchHttpClient elasticsearchHttpClient,
                                                       ObjectMapper objectMapper) {
        this.properties = properties;
        this.elasticsearchHttpClient = elasticsearchHttpClient;
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
            properties.set("memory_id", objectMapper.createObjectNode().put("type", "long"));
            properties.set("agent_id", objectMapper.createObjectNode().put("type", "long"));
            properties.set("workspace_id", objectMapper.createObjectNode().put("type", "long"));
            properties.set("user_id", objectMapper.createObjectNode().put("type", "long"));
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
            log.warn("Elasticsearch long-term memory index init skipped: {}", e.getMessage());
        }
    }

    @Override
    public void indexMemory(AgentLongTermMemory memory, float[] embedding) {
        if (memory.getEsDocumentId() == null || embedding == null || embedding.length == 0) {
            return;
        }
        try {
            ObjectNode doc = objectMapper.createObjectNode();
            doc.put("memory_id", memory.getId());
            doc.put("agent_id", memory.getAgentId());
            doc.put("workspace_id", memory.getWorkspaceId());
            doc.put("user_id", memory.getUserId());
            doc.put("content", memory.getContent());
            ArrayNode vector = objectMapper.createArrayNode();
            for (float value : embedding) {
                vector.add(value);
            }
            doc.set("embedding", vector);
            send("PUT", "/" + INDEX + "/_doc/" + memory.getEsDocumentId(), doc.toString());
        } catch (Exception e) {
            log.warn("Failed to index long-term memory {}: {}", memory.getId(), e.getMessage());
        }
    }

    @Override
    public void deleteMemory(String esDocumentId) {
        if (esDocumentId == null || esDocumentId.isBlank()) {
            return;
        }
        try {
            send("DELETE", "/" + INDEX + "/_doc/" + esDocumentId, null);
        } catch (Exception e) {
            log.warn("Failed to delete long-term memory index {}: {}", esDocumentId, e.getMessage());
        }
    }

    @Override
    public List<Long> searchByVector(Long agentId, Long userId, float[] queryEmbedding, int topK) {
        if (queryEmbedding == null || queryEmbedding.length == 0 || agentId == null || userId == null) {
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
            ObjectNode filterBool = objectMapper.createObjectNode();
            ArrayNode filters = objectMapper.createArrayNode();
            ObjectNode agentTerm = objectMapper.createObjectNode();
            agentTerm.put("agent_id", agentId);
            filters.add(objectMapper.createObjectNode().set("term", agentTerm));
            ObjectNode userTerm = objectMapper.createObjectNode();
            userTerm.put("user_id", userId);
            filters.add(objectMapper.createObjectNode().set("term", userTerm));
            filterBool.set("filter", filters);
            knn.set("filter", objectMapper.createObjectNode().set("bool", filterBool));
            ObjectNode body = objectMapper.createObjectNode();
            body.put("size", Math.max(topK, 1));
            body.set("knn", knn);
            HttpResponse<String> response = send("POST", "/" + INDEX + "/_search", body.toString());
            return parseMemoryIds(response.body());
        } catch (Exception e) {
            log.warn("Long-term memory vector search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Long> parseMemoryIds(String body) throws Exception {
        List<Long> ids = new ArrayList<>();
        var root = objectMapper.readTree(body);
        var hits = root.path("hits").path("hits");
        if (!hits.isArray()) {
            return ids;
        }
        for (var hit : hits) {
            long memoryId = hit.path("_source").path("memory_id").asLong(0L);
            if (memoryId > 0) {
                ids.add(memoryId);
            }
        }
        return ids;
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        return elasticsearchHttpClient.send(method, path, body);
    }
}
