package com.boxai.runtime.workflow.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowGraphJoinTest {

    @Test
    void findsJoinNodeForParallelBranches() {
        WorkflowNode start = new WorkflowNode("start", "Start", null);
        WorkflowNode parallel = new WorkflowNode("p", "Parallel", null);
        WorkflowNode left = new WorkflowNode("left", "Template", null);
        WorkflowNode right = new WorkflowNode("right", "Template", null);
        WorkflowNode join = new WorkflowNode("join", "Output", null);
        WorkflowGraph graph = new WorkflowGraph(
                List.of(start, parallel, left, right, join),
                List.of(
                        new WorkflowEdge("start", "p", null, null),
                        new WorkflowEdge("p", "left", "branch-0", null),
                        new WorkflowEdge("p", "right", "branch-1", null),
                        new WorkflowEdge("left", "join", null, null),
                        new WorkflowEdge("right", "join", null, null)
                ));
        assertEquals("join", graph.findJoinNode(List.of("left", "right")));
    }
}
