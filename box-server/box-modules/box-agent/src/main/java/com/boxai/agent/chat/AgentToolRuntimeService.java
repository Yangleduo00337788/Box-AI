package com.boxai.agent.chat;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentMcp;
import com.boxai.domain.agent.AgentMcpRepository;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentSubAgent;
import com.boxai.domain.agent.AgentSubAgentRepository;
import com.boxai.domain.agent.AgentTool;
import com.boxai.domain.agent.AgentToolRepository;
import com.boxai.domain.mcp.McpServer;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.tool.application.McpToolCatalogParser;
import com.boxai.tool.application.McpToolExecutor;
import com.boxai.tool.application.ToolExecutionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AgentToolRuntimeService {

    private final AgentToolRepository agentToolRepository;
    private final AgentMcpRepository agentMcpRepository;
    private final ToolRepository toolRepository;
    private final McpServerRepository mcpServerRepository;
    private final ToolExecutionService toolExecutionService;
    private final McpToolExecutor mcpToolExecutor;
    private final McpToolCatalogParser mcpToolCatalogParser;
    private final AgentSubAgentRepository agentSubAgentRepository;
    private final AgentRepository agentRepository;
    private final AgentSubAgentRuntimeService agentSubAgentRuntimeService;

    public AgentToolRuntimeService(AgentToolRepository agentToolRepository,
                                   AgentMcpRepository agentMcpRepository,
                                   ToolRepository toolRepository,
                                   McpServerRepository mcpServerRepository,
                                   ToolExecutionService toolExecutionService,
                                   McpToolExecutor mcpToolExecutor,
                                   McpToolCatalogParser mcpToolCatalogParser,
                                   AgentSubAgentRepository agentSubAgentRepository,
                                   AgentRepository agentRepository,
                                   @Lazy AgentSubAgentRuntimeService agentSubAgentRuntimeService) {
        this.agentToolRepository = agentToolRepository;
        this.agentMcpRepository = agentMcpRepository;
        this.toolRepository = toolRepository;
        this.mcpServerRepository = mcpServerRepository;
        this.toolExecutionService = toolExecutionService;
        this.mcpToolExecutor = mcpToolExecutor;
        this.mcpToolCatalogParser = mcpToolCatalogParser;
        this.agentSubAgentRepository = agentSubAgentRepository;
        this.agentRepository = agentRepository;
        this.agentSubAgentRuntimeService = agentSubAgentRuntimeService;
    }

    public List<ResolvedAgentTool> resolveTools(Long versionId) {
        List<ResolvedAgentTool> tools = new ArrayList<>();
        tools.addAll(resolveBoundTools(versionId));
        tools.addAll(resolveMcpTools(versionId));
        tools.addAll(resolveSubAgents(versionId));
        return tools;
    }

    private List<ResolvedAgentTool> resolveBoundTools(Long versionId) {
        List<AgentTool> bindings = agentToolRepository.listByVersionId(versionId);
        List<ResolvedAgentTool> tools = new ArrayList<>();
        for (AgentTool binding : bindings) {
            if (Boolean.FALSE.equals(binding.getEnabled())) {
                continue;
            }
            Tool tool = toolRepository.findById(binding.getToolId()).orElse(null);
            if (tool == null || tool.getStatus() == null || tool.getStatus() != 1) {
                continue;
            }
            if ("MCP".equalsIgnoreCase(tool.getType())) {
                continue;
            }
            tools.add(new ResolvedAgentTool(
                    tool.getId(),
                    tool.getToolKey(),
                    tool.getName(),
                    tool.getDescription(),
                    tool.getType(),
                    null,
                    null,
                    null));
        }
        return tools;
    }

    private List<ResolvedAgentTool> resolveMcpTools(Long versionId) {
        List<ResolvedAgentTool> tools = new ArrayList<>();
        for (AgentMcp binding : agentMcpRepository.listByVersionId(versionId)) {
            if (Boolean.FALSE.equals(binding.getEnabled())) {
                continue;
            }
            McpServer server = mcpServerRepository.findById(binding.getMcpServerId()).orElse(null);
            if (server == null || server.getStatus() == null || server.getStatus() != 1) {
                continue;
            }
            for (McpToolCatalogParser.McpCatalogTool catalogTool : mcpToolCatalogParser.parse(server.getToolCatalogJson())) {
                tools.add(new ResolvedAgentTool(
                        null,
                        mcpToolKey(server.getServerKey(), catalogTool.name()),
                        catalogTool.name(),
                        catalogTool.description(),
                        "MCP",
                        server.getId(),
                        catalogTool.name(),
                        null));
            }
        }
        return tools;
    }

    private List<ResolvedAgentTool> resolveSubAgents(Long versionId) {
        List<ResolvedAgentTool> tools = new ArrayList<>();
        for (AgentSubAgent binding : agentSubAgentRepository.listByVersionId(versionId)) {
            if (Boolean.FALSE.equals(binding.getEnabled())) {
                continue;
            }
            Agent subAgent = agentRepository.findById(binding.getSubAgentId()).orElse(null);
            if (subAgent == null) {
                continue;
            }
            String toolKey = AgentSubAgentRuntimeService.subAgentToolKey(String.valueOf(subAgent.getId()));
            String description = subAgent.getDescription() == null || subAgent.getDescription().isBlank()
                    ? "将任务委派给子智能体 " + subAgent.getName()
                    : subAgent.getDescription();
            tools.add(new ResolvedAgentTool(
                    null,
                    toolKey,
                    subAgent.getName(),
                    description,
                    "SUB_AGENT",
                    null,
                    null,
                    subAgent.getId()));
        }
        return tools;
    }

    public String execute(Long toolId, Map<String, Object> arguments) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在"));
        return toolExecutionService.execute(tool, arguments);
    }

    public String executeByKey(List<ResolvedAgentTool> tools, String toolKey) {
        return executeByKey(tools, toolKey, Map.of());
    }

    public String executeByKey(List<ResolvedAgentTool> tools, String toolKey, Map<String, Object> arguments) {
        ResolvedAgentTool resolved = tools.stream()
                .filter(item -> item.toolKey().equals(toolKey))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.TOOL_NOT_FOUND, "未找到工具: " + toolKey));
        if ("MCP".equals(resolved.type())) {
            McpServer server = mcpServerRepository.findById(resolved.mcpServerId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "MCP Server 不存在"));
            return mcpToolExecutor.execute(server, resolved.mcpToolName(), arguments == null ? Map.of() : arguments);
        }
        if ("SUB_AGENT".equals(resolved.type())) {
            return agentSubAgentRuntimeService.delegate(resolved.subAgentId(), arguments == null ? Map.of() : arguments);
        }
        return execute(resolved.toolId(), arguments == null ? Map.of() : arguments);
    }

    public static String mcpToolKey(String serverKey, String toolName) {
        return "mcp_" + sanitize(serverKey) + "_" + sanitize(toolName);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase().replaceAll("[^a-z0-9_]+", "_");
    }
}
