package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.core.WorkflowExecutionContext;
import com.boxai.runtime.workflow.core.WorkflowGraph;
import com.boxai.runtime.workflow.core.WorkflowNode;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.boxai.tool.application.InlineScriptExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowNodeExecutorsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InlineScriptExecutor inlineScriptExecutor = new InlineScriptExecutor(objectMapper);
    private final WorkflowTemplateRenderer templateRenderer = new WorkflowTemplateRenderer();

    @Test
    void codeNodeExecutesScript() throws Exception {
        CodeNodeExecutor executor = new CodeNodeExecutor(inlineScriptExecutor);
        WorkflowExecutionContext context = new WorkflowExecutionContext(1L, 1L, 1L, "exec-1",
                Map.of("input", Map.of("value", 2)));
        WorkflowNode node = new WorkflowNode("code-1", "Code",
                objectMapper.readTree("""
                        {"code":"function execute(args){ return String(args.input.value + 1); }"}
                        """));
        NodeExecutionResult result = executor.execute(new NodeExecutionContext(new WorkflowGraph(List.of(), List.of()), context, node));
        assertTrue(result.succeeded());
        assertEquals("3", context.getVariable("codeResult"));
    }

    @Test
    void loopNodeForeachCollectsResults() throws Exception {
        LoopNodeExecutor executor = new LoopNodeExecutor(inlineScriptExecutor);
        WorkflowExecutionContext context = new WorkflowExecutionContext(1L, 1L, 1L, "exec-2",
                Map.of("items", List.of("a", "b")));
        WorkflowNode node = new WorkflowNode("loop-1", "Loop",
                objectMapper.readTree("""
                        {"mode":"FOREACH","itemsVariable":"items","code":"function execute(args){ return args.loopItem; }"}
                        """));
        NodeExecutionResult result = executor.execute(new NodeExecutionContext(new WorkflowGraph(List.of(), List.of()), context, node));
        assertTrue(result.succeeded());
        assertEquals(List.of("a", "b"), context.getVariable("loopResults"));
    }

    @Test
    void switchNodeMatchesCase() throws Exception {
        SwitchNodeExecutor executor = new SwitchNodeExecutor();
        WorkflowExecutionContext context = new WorkflowExecutionContext(1L, 1L, 1L, "exec-switch",
                Map.of("input", Map.of("type", "vip")));
        WorkflowNode node = new WorkflowNode("switch-1", "Switch",
                objectMapper.readTree("""
                        {"variable":"input.type","operator":"equals","cases":[{"id":"vip","value":"vip"},{"id":"normal","value":"normal"}],"defaultCase":"default"}
                        """));
        NodeExecutionResult result = executor.execute(new NodeExecutionContext(new WorkflowGraph(List.of(), List.of()), context, node));
        assertTrue(result.succeeded());
        assertEquals("vip", result.branchHandle());
        assertEquals("vip", context.getVariable("switchResult"));
    }

    @Test
    void parallelNodeRunsTasks() throws Exception {
        ParallelNodeExecutor executor = new ParallelNodeExecutor(inlineScriptExecutor, templateRenderer, objectMapper);
        WorkflowExecutionContext context = new WorkflowExecutionContext(1L, 1L, 1L, "exec-3", Map.of("name", "Box"));
        WorkflowNode node = new WorkflowNode("parallel-1", "Parallel",
                objectMapper.readTree("""
                        {"tasks":[{"type":"TEMPLATE","template":"Hi {{name}}"},{"type":"CODE","code":"function execute(args){ return args.name.length; }"}]}
                        """));
        NodeExecutionResult result = executor.execute(new NodeExecutionContext(new WorkflowGraph(List.of(), List.of()), context, node));
        assertTrue(result.succeeded());
        assertEquals(List.of("Hi Box", "3"), context.getVariable("parallelResults"));
    }
}
