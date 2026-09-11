package com.boxai.runtime.workflow.engine;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.runtime.workflow.core.WorkflowEdge;
import com.boxai.runtime.workflow.core.WorkflowGraph;
import com.boxai.runtime.workflow.core.WorkflowNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WorkflowGraphParser {

    private final ObjectMapper objectMapper;

    public WorkflowGraphParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public WorkflowGraph parse(String definitionJson) {
        JsonNode root;
        try {
            root = objectMapper.readTree(definitionJson);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流定义不是合法的 JSON");
        }
        JsonNode nodesNode = root.get("nodes");
        JsonNode edgesNode = root.get("edges");
        if (nodesNode == null || !nodesNode.isArray()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流定义缺少 nodes");
        }
        if (edgesNode == null || !edgesNode.isArray()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流定义缺少 edges");
        }

        List<WorkflowNode> nodes = new ArrayList<>();
        for (JsonNode node : nodesNode) {
            String id = text(node, "id");
            String type = text(node, "type");
            if (id == null || type == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "节点缺少 id 或 type");
            }
            nodes.add(new WorkflowNode(id, type, node.get("config")));
        }

        List<WorkflowEdge> edges = new ArrayList<>();
        for (JsonNode edge : edgesNode) {
            String source = text(edge, "source");
            String target = text(edge, "target");
            if (source == null || target == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "连线缺少 source 或 target");
            }
            edges.add(new WorkflowEdge(
                    source,
                    target,
                    text(edge, "sourceHandle"),
                    edge.get("config")));
        }
        return new WorkflowGraph(nodes, edges);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }
}
