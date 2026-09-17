package com.boxai.runtime.workflow.executor;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.core.WorkflowBranchResult;
import com.boxai.runtime.workflow.core.WorkflowEdge;
import com.boxai.runtime.workflow.core.WorkflowGraph;
import com.boxai.runtime.workflow.engine.DefaultWorkflowExecutor;
import com.boxai.tool.application.InlineScriptExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LoopNodeExecutor implements NodeExecutor {

    private static final int DEFAULT_MAX_ITERATIONS = 100;

    private final InlineScriptExecutor inlineScriptExecutor;
    private final DefaultWorkflowExecutor workflowExecutor;

    public LoopNodeExecutor(InlineScriptExecutor inlineScriptExecutor,
                            @Lazy DefaultWorkflowExecutor workflowExecutor) {
        this.inlineScriptExecutor = inlineScriptExecutor;
        this.workflowExecutor = workflowExecutor;
    }

    @Override
    public String nodeType() {
        return "Loop";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        if (config == null) {
            return NodeExecutionResult.failed("Loop 节点缺少配置");
        }
        if (config.path("graphBody").asBoolean(false)) {
            return executeGraphBody(context, config);
        }

        String mode = config.path("mode").asText("FOREACH").trim().toUpperCase();
        int maxIterations = Math.min(config.path("maxIterations").asInt(DEFAULT_MAX_ITERATIONS), DEFAULT_MAX_ITERATIONS);
        String itemVariable = config.path("itemVariable").asText("loopItem");
        String indexVariable = config.path("indexVariable").asText("loopIndex");
        String outputVariable = config.path("outputVariable").asText("loopResults");
        String code = config.path("code").asText(null);
        String functionName = config.path("functionName").asText("execute");
        int timeoutMs = config.path("timeoutMs").asInt(5000);

        List<Object> results = new ArrayList<>();
        try {
            switch (mode) {
                case "COUNT" -> runCountLoop(context, config, maxIterations, itemVariable, indexVariable,
                        code, functionName, timeoutMs, results);
                case "WHILE" -> runWhileLoop(context, config, maxIterations, itemVariable, indexVariable,
                        code, functionName, timeoutMs, results);
                case "FOREACH" -> runForeachLoop(context, config, maxIterations, itemVariable, indexVariable,
                        code, functionName, timeoutMs, results);
                default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的 Loop 模式: " + mode);
            }
        } catch (BusinessException ex) {
            return NodeExecutionResult.failed(ex.getMessage());
        }

        context.executionContext().setVariable(outputVariable, results);
        context.executionContext().setVariable(indexVariable, results.size());
        return NodeExecutionResult.ok(Map.of(outputVariable, results));
    }

    private NodeExecutionResult executeGraphBody(NodeExecutionContext context, JsonNode config) {
        WorkflowGraph graph = context.graph();
        String loopNodeId = context.node().id();
        List<WorkflowEdge> outgoing = graph.outgoing(loopNodeId);
        String bodyStart = graph.resolveEdgeTarget(outgoing, "body", 0);
        String nextNodeId = graph.resolveEdgeTarget(outgoing, "next", outgoing.size() > 1 ? 1 : -1);
        if (bodyStart == null) {
            return NodeExecutionResult.failed("Loop 图模式缺少 body 分支");
        }
        if (nextNodeId == null) {
            nextNodeId = config.path("nextNodeId").asText(null);
        }
        if (nextNodeId == null) {
            return NodeExecutionResult.failed("Loop 图模式缺少 next 分支或 nextNodeId 配置");
        }

        String mode = config.path("mode").asText("FOREACH").trim().toUpperCase();
        int maxIterations = Math.min(config.path("maxIterations").asInt(DEFAULT_MAX_ITERATIONS), DEFAULT_MAX_ITERATIONS);
        String itemVariable = config.path("itemVariable").asText("loopItem");
        String indexVariable = config.path("indexVariable").asText("loopIndex");
        String outputVariable = config.path("outputVariable").asText("loopResults");
        List<Object> results = new ArrayList<>();

        try {
            switch (mode) {
                case "COUNT" -> runGraphCountLoop(context, graph, config, bodyStart, loopNodeId, maxIterations,
                        itemVariable, indexVariable, outputVariable, results);
                case "WHILE" -> runGraphWhileLoop(context, graph, config, bodyStart, loopNodeId, maxIterations,
                        itemVariable, indexVariable, outputVariable, results);
                case "FOREACH" -> runGraphForeachLoop(context, graph, config, bodyStart, loopNodeId, maxIterations,
                        itemVariable, indexVariable, outputVariable, results);
                default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Loop 图模式不支持: " + mode);
            }
        } catch (BusinessException ex) {
            return NodeExecutionResult.failed(ex.getMessage());
        }

        context.executionContext().setVariable(outputVariable, results);
        context.executionContext().setVariable(indexVariable, results.size());
        return NodeExecutionResult.okWithNext(nextNodeId, Map.of(outputVariable, results));
    }

    private void runGraphForeachLoop(NodeExecutionContext context,
                                     WorkflowGraph graph,
                                     JsonNode config,
                                     String bodyStart,
                                     String loopNodeId,
                                     int maxIterations,
                                     String itemVariable,
                                     String indexVariable,
                                     String outputVariable,
                                     List<Object> results) {
        String itemsVariable = config.path("itemsVariable").asText("items");
        Object itemsValue = context.executionContext().getVariable(itemsVariable);
        List<?> items = toList(itemsValue);
        int limit = Math.min(items.size(), maxIterations);
        for (int index = 0; index < limit; index++) {
            results.add(runGraphIteration(context, graph, bodyStart, loopNodeId, itemVariable, indexVariable, items.get(index), index));
        }
    }

    private void runGraphCountLoop(NodeExecutionContext context,
                                   WorkflowGraph graph,
                                   JsonNode config,
                                   String bodyStart,
                                   String loopNodeId,
                                   int maxIterations,
                                   String itemVariable,
                                   String indexVariable,
                                   String outputVariable,
                                   List<Object> results) {
        int count = Math.min(config.path("count").asInt(1), maxIterations);
        for (int index = 0; index < count; index++) {
            results.add(runGraphIteration(context, graph, bodyStart, loopNodeId, itemVariable, indexVariable, index, index));
        }
    }

    private void runGraphWhileLoop(NodeExecutionContext context,
                                   WorkflowGraph graph,
                                   JsonNode config,
                                   String bodyStart,
                                   String loopNodeId,
                                   int maxIterations,
                                   String itemVariable,
                                   String indexVariable,
                                   String outputVariable,
                                   List<Object> results) {
        String variable = config.path("conditionVariable").asText(indexVariable);
        String operator = config.path("operator").asText("less_than");
        String expected = config.path("value").asText("10");
        int index = 0;
        while (index < maxIterations && evaluateCondition(context, variable, operator, expected, index)) {
            results.add(runGraphIteration(context, graph, bodyStart, loopNodeId, itemVariable, indexVariable, index, index));
            index++;
        }
    }

    private Object runGraphIteration(NodeExecutionContext context,
                                     WorkflowGraph graph,
                                     String bodyStart,
                                     String loopNodeId,
                                     String itemVariable,
                                     String indexVariable,
                                     Object item,
                                     int index) {
        context.executionContext().setVariable(itemVariable, item);
        context.executionContext().setVariable(indexVariable, index);
        WorkflowBranchResult branchResult = workflowExecutor.executeBranch(
                graph,
                bodyStart,
                loopNodeId,
                context.executionContext());
        if (!branchResult.succeeded()) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, branchResult.errorMessage());
        }
        Object lastOutput = branchResult.traces().isEmpty()
                ? item
                : branchResult.traces().get(branchResult.traces().size() - 1).output();
        return lastOutput == null ? item : lastOutput;
    }

    private void runForeachLoop(NodeExecutionContext context,
                                JsonNode config,
                                int maxIterations,
                                String itemVariable,
                                String indexVariable,
                                String code,
                                String functionName,
                                int timeoutMs,
                                List<Object> results) {
        String itemsVariable = config.path("itemsVariable").asText("items");
        Object itemsValue = context.executionContext().getVariable(itemsVariable);
        List<?> items = toList(itemsValue);
        int limit = Math.min(items.size(), maxIterations);
        for (int i = 0; i < limit; i++) {
            results.add(runIteration(context, itemVariable, indexVariable, code, functionName, timeoutMs, items.get(i), i));
        }
    }

    private void runCountLoop(NodeExecutionContext context,
                              JsonNode config,
                              int maxIterations,
                              String itemVariable,
                              String indexVariable,
                              String code,
                              String functionName,
                              int timeoutMs,
                              List<Object> results) {
        int count = Math.min(config.path("count").asInt(1), maxIterations);
        for (int i = 0; i < count; i++) {
            results.add(runIteration(context, itemVariable, indexVariable, code, functionName, timeoutMs, i, i));
        }
    }

    private void runWhileLoop(NodeExecutionContext context,
                              JsonNode config,
                              int maxIterations,
                              String itemVariable,
                              String indexVariable,
                              String code,
                              String functionName,
                              int timeoutMs,
                              List<Object> results) {
        String variable = config.path("conditionVariable").asText(indexVariable);
        String operator = config.path("operator").asText("less_than");
        String expected = config.path("value").asText("10");
        int index = 0;
        while (index < maxIterations && evaluateCondition(context, variable, operator, expected, index)) {
            results.add(runIteration(context, itemVariable, indexVariable, code, functionName, timeoutMs, index, index));
            index++;
        }
    }

    private Object runIteration(NodeExecutionContext context,
                                String itemVariable,
                                String indexVariable,
                                String code,
                                String functionName,
                                int timeoutMs,
                                Object item,
                                int index) {
        context.executionContext().setVariable(itemVariable, item);
        context.executionContext().setVariable(indexVariable, index);
        if (code == null || code.isBlank()) {
            return item;
        }
        Map<String, Object> args = new HashMap<>(context.executionContext().variables());
        args.put(itemVariable, item);
        args.put(indexVariable, index);
        return inlineScriptExecutor.execute(code, functionName, args, timeoutMs);
    }

    private boolean evaluateCondition(NodeExecutionContext context,
                                      String variable,
                                      String operator,
                                      String expected,
                                      int index) {
        Object actual = context.executionContext().getVariable(variable);
        if (actual == null && indexVariableFallback(variable, index)) {
            actual = index;
        }
        String actualText = actual == null ? null : String.valueOf(actual);
        return switch (operator == null ? "less_than" : operator.toLowerCase()) {
            case "less_than", "lt", "<" -> compareNumber(actualText, expected) < 0;
            case "less_or_equal", "lte", "<=" -> compareNumber(actualText, expected) <= 0;
            case "greater_than", "gt", ">" -> compareNumber(actualText, expected) > 0;
            case "greater_or_equal", "gte", ">=" -> compareNumber(actualText, expected) >= 0;
            case "equals", "eq", "=" -> String.valueOf(actual).equals(expected);
            case "not_equals", "neq", "!=" -> !String.valueOf(actual).equals(expected);
            default -> compareNumber(actualText, expected) < 0;
        };
    }

    private boolean indexVariableFallback(String variable, int index) {
        return "loopIndex".equals(variable);
    }

    private int compareNumber(String actualText, String expected) {
        try {
            double actual = actualText == null ? 0 : Double.parseDouble(actualText);
            double expectedValue = Double.parseDouble(expected);
            return Double.compare(actual, expectedValue);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private List<?> toList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        if (value instanceof Object[] array) {
            return List.of(array);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Loop FOREACH 需要数组变量");
    }
}
