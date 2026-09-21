package com.boxai.agent.application;

import com.boxai.agent.chat.AgentToolRuntimeService;
import com.boxai.agent.chat.ConversationPluginRound;
import com.boxai.agent.chat.ResolvedAgentTool;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.mcp.McpServer;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.domain.plugin.WorkspacePluginInstall;
import com.boxai.domain.plugin.WorkspacePluginInstallRepository;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.tool.application.McpToolCatalogParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ConversationPluginApplicationService {

    private static final int MAX_PLUGINS = 8;
    private static final Set<String> COMPOSER_CATEGORIES = Set.of("skills", "tools", "mcp");

    private final PluginCatalogRepository pluginCatalogRepository;
    private final WorkspacePluginInstallRepository workspacePluginInstallRepository;
    private final ToolRepository toolRepository;
    private final McpServerRepository mcpServerRepository;
    private final McpToolCatalogParser mcpToolCatalogParser;
    private final ObjectMapper objectMapper;

    public ConversationPluginApplicationService(PluginCatalogRepository pluginCatalogRepository,
                                                WorkspacePluginInstallRepository workspacePluginInstallRepository,
                                                ToolRepository toolRepository,
                                                McpServerRepository mcpServerRepository,
                                                McpToolCatalogParser mcpToolCatalogParser,
                                                ObjectMapper objectMapper) {
        this.pluginCatalogRepository = pluginCatalogRepository;
        this.workspacePluginInstallRepository = workspacePluginInstallRepository;
        this.toolRepository = toolRepository;
        this.mcpServerRepository = mcpServerRepository;
        this.mcpToolCatalogParser = mcpToolCatalogParser;
        this.objectMapper = objectMapper;
    }

    public ConversationPluginRound resolve(Long workspaceId, List<Long> pluginIds) {
        if (pluginIds == null || pluginIds.isEmpty()) {
            return ConversationPluginRound.empty();
        }
        if (pluginIds.size() > MAX_PLUGINS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单次消息最多启用 " + MAX_PLUGINS + " 个插件");
        }
        List<ResolvedAgentTool> extraTools = new ArrayList<>();
        StringBuilder skills = new StringBuilder();
        Set<String> toolKeys = new LinkedHashSet<>();
        for (Long pluginId : pluginIds) {
            if (pluginId == null) {
                continue;
            }
            WorkspacePluginInstall install = workspacePluginInstallRepository
                    .findByWorkspaceAndPlugin(workspaceId, pluginId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "插件未安装到当前工作空间"));
            PluginCatalog plugin = pluginCatalogRepository.findById(pluginId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "插件不存在"));
            String category = normalizeCategory(plugin.getCategory());
            if (!COMPOSER_CATEGORIES.contains(category)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "对话加号仅支持技能、工具与 MCP 插件，请在工作空间智能体中配置知识库与工作流");
            }
            if ("skills".equals(category)) {
                appendSkill(skills, plugin);
                continue;
            }
            if ("tools".equals(category)) {
                extraTools.addAll(resolveToolInstall(install, toolKeys));
                continue;
            }
            if ("mcp".equals(category)) {
                extraTools.addAll(resolveMcpInstall(install, toolKeys));
            }
        }
        String skillBlock = skills.isEmpty() ? null : skills.toString().trim();
        return new ConversationPluginRound(extraTools, skillBlock);
    }

    private void appendSkill(StringBuilder skills, PluginCatalog plugin) {
        JsonNode manifest = PluginManifest.parse(plugin.getManifestJson(), objectMapper);
        String instructions = PluginManifest.text(manifest, "instructions", plugin.getDescription());
        if (instructions == null || instructions.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "技能插件缺少说明内容");
        }
        if (!skills.isEmpty()) {
            skills.append("\n\n");
        }
        skills.append("### ").append(plugin.getTitle()).append("\n").append(instructions.trim());
    }

    private List<ResolvedAgentTool> resolveToolInstall(WorkspacePluginInstall install, Set<String> toolKeys) {
        if (!"tool".equals(install.getResourceType()) || install.getResourceId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工具插件未正确安装");
        }
        Tool tool = toolRepository.findById(install.getResourceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在"));
        if (tool.getStatus() == null || tool.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工具不可用");
        }
        if (!toolKeys.add(tool.getToolKey())) {
            return List.of();
        }
        return List.of(new ResolvedAgentTool(
                tool.getId(),
                tool.getToolKey(),
                tool.getName(),
                tool.getDescription(),
                tool.getType(),
                null,
                null,
                null,
                null,
                false));
    }

    private List<ResolvedAgentTool> resolveMcpInstall(WorkspacePluginInstall install, Set<String> toolKeys) {
        if (!"mcp".equals(install.getResourceType()) || install.getResourceId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "MCP 插件未正确安装");
        }
        McpServer server = mcpServerRepository.findById(install.getResourceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "MCP Server 不存在"));
        if (server.getStatus() == null || server.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "MCP Server 不可用");
        }
        List<ResolvedAgentTool> tools = new ArrayList<>();
        for (McpToolCatalogParser.McpCatalogTool catalogTool : mcpToolCatalogParser.parse(server.getToolCatalogJson())) {
            String toolKey = AgentToolRuntimeService.mcpToolKey(server.getServerKey(), catalogTool.name());
            if (!toolKeys.add(toolKey)) {
                continue;
            }
            tools.add(new ResolvedAgentTool(
                    null,
                    toolKey,
                    catalogTool.name(),
                    catalogTool.description(),
                    "MCP",
                    server.getId(),
                    catalogTool.name(),
                    null,
                    null,
                    false));
        }
        return tools;
    }

    private static String normalizeCategory(String category) {
        if (category == null) {
            return "";
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }
}
