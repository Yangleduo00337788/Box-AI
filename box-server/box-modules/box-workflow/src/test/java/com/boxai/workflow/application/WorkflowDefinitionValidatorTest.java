package com.boxai.workflow.application;

import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.domain.platform.PlatformModelRepository;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.domain.workflow.WorkflowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowDefinitionValidatorTest {

    private final WorkflowDefinitionValidator validator = new WorkflowDefinitionValidator(
            new ObjectMapper(),
            org.mockito.Mockito.mock(AgentRepository.class),
            org.mockito.Mockito.mock(ToolRepository.class),
            org.mockito.Mockito.mock(KnowledgeBaseRepository.class),
            org.mockito.Mockito.mock(WorkflowRepository.class),
            org.mockito.Mockito.mock(PlatformModelRepository.class),
            org.mockito.Mockito.mock(McpServerRepository.class));

    @Test
    void rejectsInvalidJson() {
        var result = validator.validate("{not-json");
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(item -> item.contains("合法的 JSON")));
    }

    @Test
    void rejectsDefinitionWithoutStartNode() {
        var result = validator.validate("""
                {"nodes":[{"id":"o","type":"Output"}],"edges":[],"variables":[]}
                """);
        assertFalse(result.valid());
        assertTrue(result.errors().contains("必须包含一个 Start 节点"));
    }

    @Test
    void acceptsMinimalStartToOutputGraph() {
        var result = validator.validate("""
                {"nodes":[{"id":"s","type":"Start"},{"id":"o","type":"Output"}],"edges":[{"source":"s","target":"o"}],"variables":[]}
                """);
        assertTrue(result.valid(), () -> String.join("；", result.errors()));
    }

    @Test
    void rejectsMissingNodesArray() {
        var result = validator.validate("{\"edges\":[]}");
        assertFalse(result.valid());
        assertTrue(result.errors().contains("缺少 nodes 数组"));
    }

    @Test
    void rejectsDuplicateStartAndUnknownType() {
        var result = validator.validate("""
                {"nodes":[
                  {"id":"s1","type":"Start"},
                  {"id":"s2","type":"Start"},
                  {"id":"x","type":"Foo"},
                  {"id":"o","type":"Output"}
                ],"edges":[{"source":"s1","target":"o"},{"source":"s2","target":"x"},{"source":"x","target":"o"}]}
                """);
        assertFalse(result.valid());
        assertTrue(result.errors().contains("只能包含一个 Start 节点"));
        assertTrue(result.errors().stream().anyMatch(item -> item.contains("未知节点类型: Foo")));
    }

    @Test
    void rejectsIsolatedNodeAndCycle() {
        var isolated = validator.validate("""
                {"nodes":[{"id":"s","type":"Start"},{"id":"o","type":"Output"},{"id":"i","type":"Input"}],
                 "edges":[{"source":"s","target":"o"}]}
                """);
        assertTrue(isolated.errors().contains("存在孤立节点: i"));

        var cycle = validator.validate("""
                {"nodes":[{"id":"s","type":"Start"},{"id":"o","type":"Output"}],
                 "edges":[{"source":"s","target":"o"},{"source":"o","target":"s"}]}
                """);
        assertTrue(cycle.errors().contains("工作流存在环路"));
    }

    @Test
    void rejectsLlmWebhookToolAndConditionMisconfig() {
        var result = validator.validate("""
                {"nodes":[
                  {"id":"s","type":"Start"},
                  {"id":"llm","type":"LLM","config":{}},
                  {"id":"wh","type":"Webhook","config":{}},
                  {"id":"t","type":"Tool","config":{}},
                  {"id":"c","type":"Condition","config":{"variable":"x"}},
                  {"id":"o","type":"Output"}
                ],"edges":[
                  {"source":"s","target":"llm"},
                  {"source":"llm","target":"wh"},
                  {"source":"wh","target":"t"},
                  {"source":"t","target":"c"},
                  {"source":"c","target":"o"}
                ]}
                """);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(item -> item.contains("LLM 节点 llm 缺少 platformModelId")));
        assertTrue(result.errors().stream().anyMatch(item -> item.contains("Webhook 节点 wh 缺少 url")));
        assertTrue(result.errors().stream().anyMatch(item -> item.contains("Tool 节点 t 缺少 toolId")));
        assertTrue(result.errors().contains("Condition 节点 c 缺少 true 分支连线"));
        assertTrue(result.errors().contains("Condition 节点 c 缺少 false 分支连线"));
    }

    @Test
    void rejectsUndeclaredTemplateVariable() {
        var result = validator.validate("""
                {"nodes":[
                  {"id":"s","type":"Start"},
                  {"id":"o","type":"Output","config":{"template":"{{ unknownVar }}"}}
                ],"edges":[{"source":"s","target":"o"}],"variables":[]}
                """);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(item -> item.contains("引用了未声明变量: unknownVar")));
    }

    @Test
    void normalizeDefinitionFallsBackToEmptyGraph() {
        assertEquals("{\"nodes\":[],\"edges\":[],\"variables\":[]}", validator.normalizeDefinition("  "));
    }
}
