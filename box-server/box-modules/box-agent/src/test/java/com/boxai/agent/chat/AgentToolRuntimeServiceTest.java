package com.boxai.agent.chat;

import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.tool.application.ToolExecutionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentToolRuntimeServiceTest {

    @Test
    void executeByKeyDelegatesFunctionToolToExecutionService() {
        ToolRepository toolRepository = mock(ToolRepository.class);
        ToolExecutionService toolExecutionService = mock(ToolExecutionService.class);
        AgentToolRuntimeService service = new AgentToolRuntimeService(
                mock(com.boxai.domain.agent.AgentToolRepository.class),
                mock(com.boxai.domain.agent.AgentMcpRepository.class),
                toolRepository,
                mock(com.boxai.domain.mcp.McpServerRepository.class),
                toolExecutionService,
                mock(com.boxai.tool.application.McpToolExecutor.class),
                mock(com.boxai.tool.application.McpToolCatalogParser.class),
                mock(com.boxai.domain.agent.AgentSubAgentRepository.class),
                mock(com.boxai.domain.agent.AgentRepository.class),
                mock(AgentSubAgentRuntimeService.class));

        Tool tool = new Tool();
        tool.setId(9L);
        tool.setToolKey("calc");
        tool.setType("FUNCTION");
        tool.setStatus(1);
        when(toolRepository.findById(9L)).thenReturn(Optional.of(tool));
        when(toolExecutionService.execute(eq(tool), any())).thenReturn("42");

        List<ResolvedAgentTool> tools = List.of(new ResolvedAgentTool(
                9L, "calc", "Calc", "returns number", "FUNCTION", null, null, null));
        String result = service.executeByKey(tools, "calc", Map.of("x", 1));

        assertEquals("42", result);
        verify(toolExecutionService).execute(tool, Map.of("x", 1));
    }
}
