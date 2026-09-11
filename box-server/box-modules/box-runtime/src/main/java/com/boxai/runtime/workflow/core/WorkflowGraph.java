package com.boxai.runtime.workflow.core;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowGraph {

    private final Map<String, WorkflowNode> nodeById;
    private final Map<String, List<WorkflowEdge>> outgoingEdges;

    public WorkflowGraph(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        this.nodeById = new HashMap<>();
        for (WorkflowNode node : nodes) {
            this.nodeById.put(node.id(), node);
        }
        this.outgoingEdges = new HashMap<>();
        for (WorkflowEdge edge : edges) {
            outgoingEdges.computeIfAbsent(edge.source(), key -> new ArrayList<>()).add(edge);
        }
    }

    public WorkflowNode getNode(String nodeId) {
        WorkflowNode node = nodeById.get(nodeId);
        if (node == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "节点不存在: " + nodeId);
        }
        return node;
    }

    public WorkflowNode findStartNode() {
        List<WorkflowNode> starts = nodeById.values().stream()
                .filter(node -> "Start".equalsIgnoreCase(node.type()))
                .toList();
        if (starts.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流缺少 Start 节点");
        }
        if (starts.size() > 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流只能有一个 Start 节点");
        }
        return starts.getFirst();
    }

    public List<WorkflowEdge> outgoing(String nodeId) {
        return outgoingEdges.getOrDefault(nodeId, List.of());
    }
}
