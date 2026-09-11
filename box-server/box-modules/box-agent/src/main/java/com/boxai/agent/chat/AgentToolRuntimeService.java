package com.boxai.agent.chat;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.AgentMcp;
import com.boxai.domain.agent.AgentMcpRepository;
import com.boxai.domain.agent.AgentTool;
import com.boxai.domain.agent.AgentToolRepository;
import com.boxai.domain.mcp.McpServer;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolHttpConfig;
import com.boxai.domain.tool.ToolHttpConfigRepository;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.tool.api.ToolTestResultVO;
import com.boxai.tool.application.HttpToolExecutor;
import com.boxai.tool.application.McpToolCatalogParser;
import com.boxai.tool.application.McpToolExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgentToolRuntimeService {

    private final AgentToolRepository agentToolRepository;
    private final AgentMcpRepository agentMcpRepository;
    private final ToolRepository toolRepository;
    private final ToolHttpConfigRepository toolHttpConfigRepository;
    private final McpServerRepository mcpServerRepository;
    private final HttpToolExecutor httpToolExecutor;
    private final McpToolExecutor mcpToolExecutor;
    private final McpToolCatalogParser mcpToolCatalogParser;

    public AgentToolRuntimeService(AgentToolRepository agentToolRepository,
                                   AgentMcpRepository agentMcpRepository,
                                   ToolRepository toolRepository,
                                   ToolHttpConfigRepository toolHttpConfigRepository,
                                   McpServerRepository mcpServerRepository,
                                   HttpToolExecutor httpToolExecutor,
                                   McpToolExecutor mcpToolExecutor,
                                   McpToolCatalogParser mcpToolCatalogParser) {
        this.agentToolRepository = agentToolRepository;
        this.agentMcpRepository = agentMcpRepository;
        this.toolRepository = toolRepository;
        this.toolHttpConfigRepository = toolHttpConfigRepository;
        this.mcpServerRepository = mcpServerRepository;
        this.httpToolExecutor = httpToolExecutor;
        this.mcpToolExecutor = mcpToolExecutor;
        this.mcpToolCatalogParser = mcpToolCatalogParser;
    }

    public List<ResolvedAgentTool> resolveTools(Long versionId) {
        List<ResolvedAgentTool> tools = new ArrayList<>();
        tools.addAll(resolveHttpTools(versionId));
        tools.addAll(resolveMcpTools(versionId));
        return tools;
    }

    private List<ResolvedAgentTool> resolveHttpTools(Long versionId) {
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
            tools.add(new ResolvedAgentTool(
                    tool.getId(),
                    tool.getToolKey(),
                    tool.getName(),
                    tool.getDescription(),
                    tool.getType(),
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
                        catalogTool.name()));
            }
        }
        return tools;
    }

    public String execute(Long toolId) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在"));
        if (!"HTTP".equals(tool.getType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前仅支持 HTTP 工具调用");
        }
        ToolHttpConfig config = toolHttpConfigRepository.findByToolId(tool.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "HTTP 工具未配置"));
        ToolTestResultVO result = httpToolExecutor.execute(config);
        return result.body() == null ? "" : result.body();
    }

    public String executeByKey(List<ResolvedAgentTool> tools, String toolKey) {
        ResolvedAgentTool resolved = tools.stream()
                .filter(item -> item.toolKey().equals(toolKey))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.TOOL_NOT_FOUND, "未找到工具: " + toolKey));
        if ("MCP".equals(resolved.type())) {
            McpServer server = mcpServerRepository.findById(resolved.mcpServerId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "MCP Server 不存在"));
            return mcpToolExecutor.execute(server, resolved.mcpToolName());
        }
        return execute(resolved.toolId());
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
