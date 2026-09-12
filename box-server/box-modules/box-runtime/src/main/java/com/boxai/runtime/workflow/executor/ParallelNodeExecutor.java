package com.boxai.runtime.workflow.executor;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.boxai.tool.application.InlineScriptExecutor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class ParallelNodeExecutor implements NodeExecutor {

    private final InlineScriptExecutor inlineScriptExecutor;
    private final WorkflowTemplateRenderer templateRenderer;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ParallelNodeExecutor(InlineScriptExecutor inlineScriptExecutor,
                                WorkflowTemplateRenderer templateRenderer,
                                ObjectMapper objectMapper) {
        this.inlineScriptExecutor = inlineScriptExecutor;
        this.templateRenderer = templateRenderer;
        this.objectMapper = objectMapper;
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
        String outputVariable = config.path("outputVariable").asText("parallelResults");
        int timeoutMs = config.path("timeoutMs").asInt(10000);
        List<Map<String, Object>> tasks = parseTasks(config);
        if (tasks.isEmpty()) {
            return NodeExecutionResult.failed("Parallel 节点至少需要一个任务");
        }
        Map<String, Object> variables = context.executionContext().variables();
        List<Callable<Object>> callables = tasks.stream()
                .map(task -> (Callable<Object>) () -> executeTask(task, variables))
                .toList();
        try {
            List<Future<Object>> futures = executor.invokeAll(callables);
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

    private List<Map<String, Object>> parseTasks(JsonNode config) {
        JsonNode tasksNode = config.get("tasks");
        if (tasksNode == null || tasksNode.isNull()) {
            String tasksJson = config.path("tasksJson").asText("[]");
            try {
                return objectMapper.readValue(tasksJson, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ex) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Parallel tasksJson 不是合法 JSON 数组");
            }
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
}
