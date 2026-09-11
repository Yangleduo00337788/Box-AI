package com.boxai.runtime.workflow.engine;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.core.WorkflowEdge;
import com.boxai.runtime.workflow.core.WorkflowExecutionContext;
import com.boxai.runtime.workflow.core.WorkflowExecutionResult;
import com.boxai.runtime.workflow.core.WorkflowGraph;
import com.boxai.runtime.workflow.core.WorkflowNode;
import com.boxai.runtime.workflow.core.WorkflowNodeTrace;
import com.boxai.runtime.workflow.executor.NodeExecutor;
import com.boxai.runtime.workflow.executor.NodeExecutorRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultWorkflowExecutor {

    private static final int MAX_STEPS = 200;

    private final WorkflowGraphParser graphParser;
    private final NodeExecutorRegistry nodeExecutorRegistry;

    public DefaultWorkflowExecutor(WorkflowGraphParser graphParser, NodeExecutorRegistry nodeExecutorRegistry) {
        this.graphParser = graphParser;
        this.nodeExecutorRegistry = nodeExecutorRegistry;
    }

    public WorkflowExecutionResult execute(String definitionJson, WorkflowExecutionContext executionContext) {
        WorkflowGraph graph = graphParser.parse(definitionJson);
        List<WorkflowNodeTrace> traces = new ArrayList<>();
        String currentNodeId = graph.findStartNode().id();
        int steps = 0;

        while (currentNodeId != null) {
            if (++steps > MAX_STEPS) {
                return failed(executionContext, traces, "工作流执行超过最大步数限制");
            }
            WorkflowNode node = graph.getNode(currentNodeId);
            NodeExecutor executor = nodeExecutorRegistry.get(node.type());
            long started = System.currentTimeMillis();
            NodeExecutionResult result = executor.execute(new NodeExecutionContext(graph, executionContext, node));
            long durationMs = System.currentTimeMillis() - started;

            if (!result.succeeded()) {
                traces.add(new WorkflowNodeTrace(
                        node.id(), node.type(), "FAILED", durationMs, result.nodeOutput(), result.errorMessage()));
                return failed(executionContext, traces, result.errorMessage());
            }

            traces.add(new WorkflowNodeTrace(
                    node.id(), node.type(), "SUCCEEDED", durationMs, result.nodeOutput(), null));
            if (result.terminal()) {
                return succeeded(executionContext, traces);
            }
            currentNodeId = resolveNext(graph, currentNodeId, result);
        }

        if (executionContext.outputs().isEmpty()) {
            return failed(executionContext, traces, "工作流未到达 Output 节点");
        }
        return succeeded(executionContext, traces);
    }

    private String resolveNext(WorkflowGraph graph, String currentNodeId, NodeExecutionResult result) {
        List<WorkflowEdge> edges = graph.outgoing(currentNodeId);
        if (edges.isEmpty()) {
            return null;
        }
        if (result.branchHandle() != null) {
            for (WorkflowEdge edge : edges) {
                if (edge.sourceHandle() != null
                        && result.branchHandle().equalsIgnoreCase(edge.sourceHandle())) {
                    return edge.target();
                }
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST, "条件节点缺少匹配分支: " + result.branchHandle());
        }
        if (edges.size() > 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "节点存在多条出边但未指定分支: " + currentNodeId);
        }
        return edges.getFirst().target();
    }

    private WorkflowExecutionResult succeeded(WorkflowExecutionContext context, List<WorkflowNodeTrace> traces) {
        return new WorkflowExecutionResult(
                context.executionId(),
                context.executionNo(),
                "SUCCEEDED",
                context.outputs(),
                traces,
                null);
    }

    private WorkflowExecutionResult failed(WorkflowExecutionContext context,
                                           List<WorkflowNodeTrace> traces,
                                           String errorMessage) {
        return new WorkflowExecutionResult(
                context.executionId(),
                context.executionNo(),
                "FAILED",
                context.outputs(),
                traces,
                errorMessage);
    }
}
