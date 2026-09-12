package com.boxai.workflow.application;

import com.boxai.workflow.api.WorkflowValidateVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class WorkflowDefinitionValidator {

    private static final String EMPTY_DEFINITION = "{\"nodes\":[],\"edges\":[],\"variables\":[]}";

    private final ObjectMapper objectMapper;

    public WorkflowDefinitionValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String normalizeDefinition(String definitionJson) {
        if (definitionJson == null || definitionJson.isBlank()) {
            return EMPTY_DEFINITION;
        }
        return definitionJson;
    }

    public WorkflowValidateVO validate(String definitionJson) {
        List<String> errors = new ArrayList<>();
        JsonNode root;
        try {
            root = objectMapper.readTree(normalizeDefinition(definitionJson));
        } catch (Exception ex) {
            errors.add("工作流定义不是合法的 JSON");
            return new WorkflowValidateVO(false, errors);
        }
        if (!root.isObject()) {
            errors.add("工作流定义必须是 JSON 对象");
            return new WorkflowValidateVO(false, errors);
        }

        JsonNode nodes = root.get("nodes");
        JsonNode edges = root.get("edges");
        if (nodes == null || !nodes.isArray()) {
            errors.add("缺少 nodes 数组");
            return new WorkflowValidateVO(false, errors);
        }
        if (edges == null || !edges.isArray()) {
            errors.add("缺少 edges 数组");
            return new WorkflowValidateVO(false, errors);
        }

        Map<String, JsonNode> nodeMap = new HashMap<>();
        int startCount = 0;
        int outputCount = 0;
        for (JsonNode node : nodes) {
            if (!node.isObject()) {
                errors.add("nodes 中存在非法节点");
                continue;
            }
            JsonNode idNode = node.get("id");
            JsonNode typeNode = node.get("type");
            if (idNode == null || idNode.asText().isBlank()) {
                errors.add("存在未设置 id 的节点");
                continue;
            }
            String id = idNode.asText();
            if (nodeMap.containsKey(id)) {
                errors.add("存在重复节点 id: " + id);
                continue;
            }
            nodeMap.put(id, node);
            String type = typeNode == null ? "" : typeNode.asText();
            if ("Start".equalsIgnoreCase(type)) {
                startCount++;
            }
            if ("Output".equalsIgnoreCase(type)) {
                outputCount++;
            }
            if ("Tool".equalsIgnoreCase(type)) {
                validateToolNode(id, node, errors);
            }
            if ("Webhook".equalsIgnoreCase(type)) {
                validateWebhookNode(id, node, errors);
            }
            if ("SubWorkflow".equalsIgnoreCase(type)) {
                validateSubWorkflowNode(id, node, errors);
            }
            if ("Agent".equalsIgnoreCase(type)) {
                validateAgentNode(id, node, errors);
            }
        }

        if (startCount == 0) {
            errors.add("必须包含一个 Start 节点");
        }
        if (startCount > 1) {
            errors.add("只能包含一个 Start 节点");
        }
        if (outputCount == 0) {
            errors.add("必须包含至少一个 Output 节点");
        }

        Set<String> connected = new HashSet<>();
        Map<String, List<String>> adjacency = new HashMap<>();
        for (JsonNode edge : edges) {
            if (!edge.isObject()) {
                errors.add("edges 中存在非法连线");
                continue;
            }
            JsonNode sourceNode = edge.get("source");
            JsonNode targetNode = edge.get("target");
            if (sourceNode == null || targetNode == null) {
                errors.add("存在缺少 source/target 的连线");
                continue;
            }
            String source = sourceNode.asText();
            String target = targetNode.asText();
            if (!nodeMap.containsKey(source)) {
                errors.add("连线引用了不存在的源节点: " + source);
            }
            if (!nodeMap.containsKey(target)) {
                errors.add("连线引用了不存在的目标节点: " + target);
            }
            connected.add(source);
            connected.add(target);
            adjacency.computeIfAbsent(source, key -> new ArrayList<>()).add(target);
        }

        for (String nodeId : nodeMap.keySet()) {
            JsonNode node = nodeMap.get(nodeId);
            String type = node.path("type").asText("");
            if ("Start".equalsIgnoreCase(type)) {
                continue;
            }
            if (!connected.contains(nodeId)) {
                errors.add("存在孤立节点: " + nodeId);
            }
        }

        if (hasCycle(adjacency, nodeMap.keySet())) {
            errors.add("工作流存在环路");
        }

        return new WorkflowValidateVO(errors.isEmpty(), errors);
    }

    private void validateAgentNode(String nodeId, JsonNode node, List<String> errors) {
        JsonNode config = node.get("config");
        if (config == null || !config.isObject()) {
            errors.add("Agent 节点 " + nodeId + " 缺少 config");
            return;
        }
        if (!config.has("agentId")) {
            errors.add("Agent 节点 " + nodeId + " 缺少 agentId");
        }
    }

    private void validateSubWorkflowNode(String nodeId, JsonNode node, List<String> errors) {
        JsonNode config = node.get("config");
        if (config == null || !config.isObject()) {
            errors.add("Sub Workflow 节点 " + nodeId + " 缺少 config");
            return;
        }
        if (!config.has("workflowId")) {
            errors.add("Sub Workflow 节点 " + nodeId + " 缺少 workflowId");
        }
    }

    private void validateWebhookNode(String nodeId, JsonNode node, List<String> errors) {
        JsonNode config = node.get("config");
        if (config == null || !config.isObject()) {
            errors.add("Webhook 节点 " + nodeId + " 缺少 config");
            return;
        }
        if (!config.has("url") || config.path("url").asText("").isBlank()) {
            errors.add("Webhook 节点 " + nodeId + " 缺少 url");
        }
    }

    private void validateToolNode(String nodeId, JsonNode node, List<String> errors) {
        JsonNode config = node.get("config");
        if (config == null || !config.isObject()) {
            errors.add("Tool 节点 " + nodeId + " 缺少 config");
            return;
        }
        String sourceType = config.path("sourceType").asText("HTTP").trim().toUpperCase();
        if ("MCP".equals(sourceType)) {
            if (!config.has("mcpServerId")) {
                errors.add("Tool 节点 " + nodeId + " 缺少 mcpServerId");
            }
            if (!config.has("mcpToolName") || config.path("mcpToolName").asText("").isBlank()) {
                errors.add("Tool 节点 " + nodeId + " 缺少 mcpToolName");
            }
            return;
        }
        if (!config.has("toolId")) {
            errors.add("Tool 节点 " + nodeId + " 缺少 toolId");
        }
    }

    private boolean hasCycle(Map<String, List<String>> adjacency, Set<String> nodeIds) {
        Map<String, Integer> state = new HashMap<>();
        for (String nodeId : nodeIds) {
            if (state.getOrDefault(nodeId, 0) == 0 && dfsCycle(nodeId, adjacency, state)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfsCycle(String node, Map<String, List<String>> adjacency, Map<String, Integer> state) {
        int current = state.getOrDefault(node, 0);
        if (current == 1) {
            return true;
        }
        if (current == 2) {
            return false;
        }
        state.put(node, 1);
        for (String next : adjacency.getOrDefault(node, List.of())) {
            if (dfsCycle(next, adjacency, state)) {
                return true;
            }
        }
        state.put(node, 2);
        return false;
    }
}
