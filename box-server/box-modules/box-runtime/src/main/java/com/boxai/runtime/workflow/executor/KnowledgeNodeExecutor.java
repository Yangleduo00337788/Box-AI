package com.boxai.runtime.workflow.executor;

import com.boxai.knowledge.api.KnowledgeSearchHitVO;
import com.boxai.knowledge.application.KnowledgeSearchService;
import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class KnowledgeNodeExecutor implements NodeExecutor {

    private final WorkflowTemplateRenderer templateRenderer;
    private final KnowledgeSearchService knowledgeSearchService;

    public KnowledgeNodeExecutor(WorkflowTemplateRenderer templateRenderer,
                                 KnowledgeSearchService knowledgeSearchService) {
        this.templateRenderer = templateRenderer;
        this.knowledgeSearchService = knowledgeSearchService;
    }

    @Override
    public String nodeType() {
        return "Knowledge";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        if (config == null || !config.has("knowledgeBaseId")) {
            return NodeExecutionResult.failed("Knowledge 节点缺少 knowledgeBaseId");
        }
        long knowledgeBaseId = config.get("knowledgeBaseId").asLong();
        String query = templateRenderer.render(
                config.path("query").asText(config.path("prompt").asText("")),
                context.executionContext().variables());
        if (query.isBlank()) {
            return NodeExecutionResult.failed("Knowledge 节点缺少 query");
        }
        int topK = Math.max(1, config.path("topK").asInt(5));
        String outputVariable = config.path("outputVariable").asText("knowledgeResult");
        try {
            List<KnowledgeSearchHitVO> hits = knowledgeSearchService.searchHits(
                    knowledgeBaseId, query, topK, "HYBRID", null, true);
            List<Map<String, Object>> chunks = hits.stream()
                    .map(hit -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("chunkId", hit.chunkId());
                        item.put("documentId", hit.documentId());
                        item.put("content", hit.content());
                        item.put("score", hit.score());
                        return item;
                    })
                    .toList();
            Map<String, Object> output = Map.of(
                    "query", query,
                    "chunks", chunks,
                    "context", hits.stream().map(KnowledgeSearchHitVO::content).reduce((a, b) -> a + "\n\n" + b).orElse(""));
            context.executionContext().setVariable(outputVariable, output);
            return NodeExecutionResult.ok(Map.of(outputVariable, output));
        } catch (Exception ex) {
            return NodeExecutionResult.failed("Knowledge 节点执行失败: " + ex.getMessage());
        }
    }
}
