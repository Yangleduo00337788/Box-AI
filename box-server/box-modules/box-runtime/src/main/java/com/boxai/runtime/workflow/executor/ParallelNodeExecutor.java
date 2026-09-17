package com.boxai.runtime.workflow.executor;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.core.WorkflowBranchResult;
import com.boxai.runtime.workflow.core.WorkflowEdge;
import com.boxai.runtime.workflow.core.WorkflowExecutionContext;
import com.boxai.runtime.workflow.core.WorkflowGraph;
import com.boxai.runtime.workflow.engine.DefaultWorkflowExecutor;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.tool.application.InlineScriptExecutor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class ParallelNodeExecutor implements NodeExecutor {

    private static final int MAX_BRANCHES = 8;

    private final InlineScriptExecutor inlineScriptExecutor;
    private final WorkflowTemplateRenderer templateRenderer;
    private final ObjectMapper objectMapper;
    private final DefaultWorkflowExecutor workflowExecutor;
    private final ExecutorService branchExecutor = Executors.newFixedThreadPool(
            Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors())),
            runnable -> {
                Thread thread = new Thread(runnable, "box-workflow-parallel");
                thread.setDaemon(true);
                return thread;
            });

    public ParallelNodeExecutor(InlineScriptExecutor inlineScriptExecutor,
                                WorkflowTemplateRenderer templateRenderer,
                                ObjectMapper objectMapper,
                                @Lazy DefaultWorkflowExecutor workflowExecutor) {
        this.inlineScriptExecutor = inlineScriptExecutor;
        this.templateRenderer = templateRenderer;
        this.objectMapper = objectMapper;
        this.workflowExecutor = workflowExecutor;
    }

    @Override
    public String nodeType() {
        return "Parallel";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        if (config == null) {
            return NodeExecutionResult.failed("Parallel 节点缺少配置");
        }
        WorkflowGraph graph = context.graph();
        List<WorkflowEdge> outgoing = graph.outgoing(context.node().id());
        if (shouldUseGraphBranches(config, outgoing)) {
            return executeGraphBranches(context, config, graph, outgoing);
        }

        String outputVariable = config.path("outputVariable").asText("parallelResults");
        int timeoutMs = config.path("timeoutMs").asInt(10000);
        List<Map<String, Object>> tasks = parseTasks(config);
        if (tasks.isEmpty()) {
            return NodeExecutionResult.failed("Parallel 节点至少需要一个任务或图分支");
        }
        Map<String, Object> variables = context.executionContext().variables();
        List<Callable<Object>> callables = tasks.stream()
                .map(task -> (Callable<Object>) () -> executeTask(task, variables))
                .toList();
        try {
            List<Future<Object>> futures = branchExecutor.invokeAll(callables);
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get(timeoutMs, TimeUnit.MILLISECONDS));
            }
            context.executionContext().setVariable(outputVariable, results);
            return NodeExecutionResult.ok(Map.of(outputVariable, results));
        } catch (BusinessException ex) {
            return NodeExecutionResult.failed(ex.getMessage());
        } catch (Exception ex) {
            return NodeExecutionResult.failed("Parallel 节点执行失败: " + ex.getMessage());
        }
    }

    private NodeExecutionResult executeGraphBranches(NodeExecutionContext context,
                                                     JsonNode config,
                                                     WorkflowGraph graph,
                                                     List<WorkflowEdge> outgoing) {
        if (outgoing.size() > MAX_BRANCHES) {
            return NodeExecutionResult.failed("Parallel 图分支最多支持 " + MAX_BRANCHES + " 条");
        }
        List<String> branchStarts = outgoing.stream().map(WorkflowEdge::target).toList();
        String joinNodeId = text(config, "joinNodeId");
        if (joinNodeId == null) {
            joinNodeId = graph.findJoinNode(branchStarts);
        }
        if (joinNodeId == null) {
            return NodeExecutionResult.failed("Parallel 图分支缺少汇合节点，请连线到同一节点或配置 joinNodeId");
        }
        for (String branchStart : branchStarts) {
            if (!graph.isReachable(branchStart, joinNodeId)) {
                return NodeExecutionResult.failed("Parallel 分支无法到达汇合节点: " + branchStart);
            }
        }

        int timeoutMs = config.path("timeoutMs").asInt(120000);
        String outputVariable = config.path("outputVariable").asText("parallelResults");
        String stopAtJoin = joinNodeId;
        WorkspaceContext workspaceContext = WorkspaceContext.get();
        SecurityContext securityContext = SecurityContextHolder.getContext();
        List<Callable<BranchRun>> callables = new ArrayList<>();
        for (WorkflowEdge edge : outgoing) {
            String branchStart = edge.target();
            callables.add(() -> {
                if (workspaceContext != null) {
                    WorkspaceContext.set(workspaceContext);
                }
                SecurityContextHolder.setContext(securityContext);
                try {
                    WorkflowExecutionContext branchContext = context.executionContext().snapshot();
                    WorkflowBranchResult branchResult = workflowExecutor.executeBranch(
                            graph,
                            branchStart,
                            stopAtJoin,
                            branchContext);
                    return new BranchRun(branchResult, branchContext);
                } finally {
                    WorkspaceContext.clear();
                    SecurityContextHolder.clearContext();
                }
            });
        }
        try {
            List<Future<BranchRun>> futures = branchExecutor.invokeAll(callables);
            List<Map<String, Object>> branchOutputs = new ArrayList<>();
            for (Future<BranchRun> future : futures) {
                BranchRun branchRun = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                if (!branchRun.result().succeeded()) {
                    return NodeExecutionResult.failed(branchRun.result().errorMessage());
                }
                context.executionContext().mergeFrom(branchRun.context());
                Map<String, Object> branchOutput = new LinkedHashMap<>();
                branchOutput.put("traces", branchRun.result().traces());
                branchOutputs.add(branchOutput);
            }
            context.executionContext().setVariable(outputVariable, branchOutputs);
            return NodeExecutionResult.okWithNext(joinNodeId, Map.of(outputVariable, branchOutputs));
        } catch (BusinessException ex) {
            return NodeExecutionResult.failed(ex.getMessage());
        } catch (Exception ex) {
            return NodeExecutionResult.failed("Parallel 图分支执行失败: " + ex.getMessage());
        }
    }

    private record BranchRun(WorkflowBranchResult result, WorkflowExecutionContext context) {
    }

    private boolean shouldUseGraphBranches(JsonNode config, List<WorkflowEdge> outgoing) {
        if (config.path("useGraphBranches").asBoolean(false)) {
            return outgoing.size() > 1;
        }
        List<Map<String, Object>> tasks = parseTasks(config);
        return tasks.isEmpty() && outgoing.size() > 1;
    }

    private List<Map<String, Object>> parseTasks(JsonNode config) {
        JsonNode tasksNode = config.get("tasks");
        if (tasksNode == null || tasksNode.isNull()) {
            String tasksJson = config.path("tasksJson").asText("");
            if (tasksJson == null || tasksJson.isBlank() || "[]".equals(tasksJson.trim())) {
                return List.of();
            }
            try {
                return objectMapper.readValue(tasksJson, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ex) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Parallel tasksJson 不是合法 JSON 数组");
            }
        }
        if (tasksNode.isArray() && tasksNode.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.convertValue(tasksNode, new TypeReference<List<Map<String, Object>>>() {});
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Parallel tasks 配置无效");
        }
    }

    private Object executeTask(Map<String, Object> task, Map<String, Object> variables) {
        String type = String.valueOf(task.getOrDefault("type", "TEMPLATE")).trim().toUpperCase();
        return switch (type) {
            case "CODE" -> {
                String code = String.valueOf(task.getOrDefault("code", ""));
                String functionName = String.valueOf(task.getOrDefault("functionName", "execute"));
                int timeoutMs = task.get("timeoutMs") instanceof Number number ? number.intValue() : 5000;
                Map<String, Object> args = new HashMap<>(variables);
                yield inlineScriptExecutor.execute(code, functionName, args, timeoutMs);
            }
            case "TEMPLATE" -> {
                String template = String.valueOf(task.getOrDefault("template", ""));
                yield templateRenderer.render(template, variables);
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Parallel 任务类型不支持: " + type);
        };
    }

    private String text(JsonNode config, String field) {
        JsonNode value = config.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }
}
