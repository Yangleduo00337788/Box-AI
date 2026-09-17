package com.boxai.runtime.workflow.engine;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.core.WorkflowBranchResult;
import com.boxai.runtime.workflow.core.WorkflowEdge;
import com.boxai.runtime.workflow.core.WorkflowExecutionContext;
import com.boxai.runtime.workflow.core.WorkflowExecutionListener;
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
        return execute(definitionJson, executionContext, null);
    }

    public WorkflowExecutionResult execute(String definitionJson,
                                           WorkflowExecutionContext executionContext,
                                           WorkflowExecutionListener listener) {
        WorkflowGraph graph = graphParser.parse(definitionJson);
        if (listener != null) {
            executionContext.setListener(listener);
        }
        return runGraph(graph, graph.findStartNode().id(), null, executionContext, new ArrayList<>());
    }

    public WorkflowBranchResult executeBranch(WorkflowGraph graph,
                                              String startNodeId,
                                              String stopBeforeNodeId,
                                              WorkflowExecutionContext executionContext) {
        List<WorkflowNodeTrace> traces = new ArrayList<>();
        WorkflowExecutionResult result = runGraph(graph, startNodeId, stopBeforeNodeId, executionContext, traces);
        if ("SUCCEEDED".equals(result.status())) {
            return WorkflowBranchResult.success(traces);
        }
        return WorkflowBranchResult.failure(traces, result.errorMessage());
    }

    private WorkflowExecutionResult runGraph(WorkflowGraph graph,
                                             String startNodeId,
                                             String stopBeforeNodeId,
                                             WorkflowExecutionContext executionContext,
                                             List<WorkflowNodeTrace> traces) {
        String currentNodeId = startNodeId;
        int steps = 0;

        while (currentNodeId != null) {
            if (stopBeforeNodeId != null && stopBeforeNodeId.equals(currentNodeId)) {
                return succeeded(executionContext, traces);
            }
            if (++steps > MAX_STEPS) {
                return failed(executionContext, traces, "工作流执行超过最大步数限制");
            }
            WorkflowNode node = graph.getNode(currentNodeId);
            WorkflowExecutionListener listener = executionContext.listener();
            if (listener != null) {
                listener.onNodeStart(node.id(), node.type());
            }
            NodeExecutor executor = nodeExecutorRegistry.get(node.type());
            long started = System.currentTimeMillis();
            NodeExecutionResult result = executor.execute(new NodeExecutionContext(graph, executionContext, node));
            long durationMs = System.currentTimeMillis() - started;

            WorkflowNodeTrace trace = new WorkflowNodeTrace(
                    node.id(),
                    node.type(),
                    result.succeeded() ? "SUCCEEDED" : "FAILED",
                    durationMs,
                    result.nodeOutput(),
                    result.errorMessage());
            traces.add(trace);
            if (listener != null) {
                listener.onNodeComplete(trace);
            }

            if (!result.succeeded()) {
                return failed(executionContext, traces, result.errorMessage());
            }
            if (result.terminal()) {
                return succeeded(executionContext, traces);
            }
            currentNodeId = resolveNext(graph, currentNodeId, result);
        }

        if (stopBeforeNodeId != null) {
            return succeeded(executionContext, traces);
        }
        if (executionContext.outputs().isEmpty()) {
            return failed(executionContext, traces, "工作流未到达 Output 节点");
        }
        return succeeded(executionContext, traces);
    }

    private String resolveNext(WorkflowGraph graph, String currentNodeId, NodeExecutionResult result) {
        if (result.nextNodeId() != null && !result.nextNodeId().isBlank()) {
            return result.nextNodeId();
        }
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
