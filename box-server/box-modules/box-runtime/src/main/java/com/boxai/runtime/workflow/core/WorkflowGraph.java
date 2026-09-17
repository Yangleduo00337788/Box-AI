package com.boxai.runtime.workflow.core;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorkflowGraph {

    private final Map<String, WorkflowNode> nodeById;
    private final Map<String, List<WorkflowEdge>> outgoingEdges;
    private final Map<String, List<WorkflowEdge>> incomingEdges;

    public WorkflowGraph(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        this.nodeById = new HashMap<>();
        for (WorkflowNode node : nodes) {
            this.nodeById.put(node.id(), node);
        }
        this.outgoingEdges = new HashMap<>();
        this.incomingEdges = new HashMap<>();
        for (WorkflowEdge edge : edges) {
            outgoingEdges.computeIfAbsent(edge.source(), key -> new ArrayList<>()).add(edge);
            incomingEdges.computeIfAbsent(edge.target(), key -> new ArrayList<>()).add(edge);
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

    public List<WorkflowEdge> incoming(String nodeId) {
        return incomingEdges.getOrDefault(nodeId, List.of());
    }

    public String resolveEdgeTarget(List<WorkflowEdge> edges, String sourceHandle, int fallbackIndex) {
        if (edges == null || edges.isEmpty()) {
            return null;
        }
        if (sourceHandle != null && !sourceHandle.isBlank()) {
            for (WorkflowEdge edge : edges) {
                if (sourceHandle.equalsIgnoreCase(edge.sourceHandle())) {
                    return edge.target();
                }
            }
        }
        if (fallbackIndex >= 0 && fallbackIndex < edges.size()) {
            return edges.get(fallbackIndex).target();
        }
        return null;
    }

    public String findJoinNode(List<String> branchStarts) {
        if (branchStarts == null || branchStarts.isEmpty()) {
            return null;
        }
        Map<String, Integer> distanceSum = new HashMap<>();
        Map<String, Integer> seenFrom = new HashMap<>();
        for (String start : branchStarts) {
            if (start == null || start.isBlank()) {
                continue;
            }
            Map<String, Integer> distances = bfsDistances(start);
            for (Map.Entry<String, Integer> entry : distances.entrySet()) {
                if (branchStarts.contains(entry.getKey())) {
                    continue;
                }
                distanceSum.merge(entry.getKey(), entry.getValue(), Integer::sum);
                seenFrom.merge(entry.getKey(), 1, Integer::sum);
            }
        }
        int branchCount = (int) branchStarts.stream().filter(item -> item != null && !item.isBlank()).count();
        return seenFrom.entrySet().stream()
                .filter(entry -> entry.getValue() == branchCount)
                .min(Comparator.comparingInt(entry -> distanceSum.getOrDefault(entry.getKey(), Integer.MAX_VALUE)))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private Map<String, Integer> bfsDistances(String start) {
        Map<String, Integer> distances = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(start);
        distances.put(start, 0);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDistance = distances.get(current);
            for (WorkflowEdge edge : outgoing(current)) {
                String next = edge.target();
                if (distances.containsKey(next)) {
                    continue;
                }
                distances.put(next, currentDistance + 1);
                queue.add(next);
            }
        }
        return distances;
    }

    public boolean isReachable(String fromNodeId, String toNodeId) {
        if (fromNodeId == null || toNodeId == null) {
            return false;
        }
        if (fromNodeId.equals(toNodeId)) {
            return true;
        }
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(fromNodeId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }
            for (WorkflowEdge edge : outgoing(current)) {
                if (toNodeId.equals(edge.target())) {
                    return true;
                }
                queue.add(edge.target());
            }
        }
        return false;
    }
}
