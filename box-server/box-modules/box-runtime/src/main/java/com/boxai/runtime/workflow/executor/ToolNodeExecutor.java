package com.boxai.runtime.workflow.executor;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.mcp.McpServer;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolHttpConfig;
import com.boxai.domain.tool.ToolHttpConfigRepository;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tool.api.ToolTestResultVO;
import com.boxai.tool.application.HttpToolExecutor;
import com.boxai.tool.application.McpToolExecutor;
import com.boxai.tool.application.ToolExecutionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ToolNodeExecutor implements NodeExecutor {

    private final ToolRepository toolRepository;
    private final ToolHttpConfigRepository toolHttpConfigRepository;
    private final McpServerRepository mcpServerRepository;
    private final HttpToolExecutor httpToolExecutor;
    private final McpToolExecutor mcpToolExecutor;
    private final ToolExecutionService toolExecutionService;
    private final WorkflowTemplateRenderer templateRenderer;
    private final WorkspacePermissionService workspacePermissionService;
    private final ObjectMapper objectMapper;

    public ToolNodeExecutor(ToolRepository toolRepository,
                            ToolHttpConfigRepository toolHttpConfigRepository,
                            McpServerRepository mcpServerRepository,
                            HttpToolExecutor httpToolExecutor,
                            McpToolExecutor mcpToolExecutor,
                            ToolExecutionService toolExecutionService,
                            WorkflowTemplateRenderer templateRenderer,
                            WorkspacePermissionService workspacePermissionService,
                            ObjectMapper objectMapper) {
        this.toolRepository = toolRepository;
        this.toolHttpConfigRepository = toolHttpConfigRepository;
        this.mcpServerRepository = mcpServerRepository;
        this.httpToolExecutor = httpToolExecutor;
        this.mcpToolExecutor = mcpToolExecutor;
        this.toolExecutionService = toolExecutionService;
        this.templateRenderer = templateRenderer;
        this.workspacePermissionService = workspacePermissionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String nodeType() {
        return "Tool";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        workspacePermissionService.requirePermission(com.boxai.common.constant.PermissionCodes.TOOL_EXECUTE);
        JsonNode config = context.node().config();
        if (config == null) {
            return NodeExecutionResult.failed("Tool 节点缺少配置");
        }
        String outputVariable = config.path("outputVariable").asText("toolResult");
        String sourceType = config.path("sourceType").asText("HTTP").trim().toUpperCase();
        Map<String, Object> variables = context.executionContext().variables();
        try {
            Map<String, Object> output = "MCP".equals(sourceType)
                    ? executeMcp(config, variables)
                    : executeRegistryTool(config, variables);
            context.executionContext().setVariable(outputVariable, output);
            return NodeExecutionResult.ok(Map.of(outputVariable, output));
        } catch (BusinessException ex) {
            return NodeExecutionResult.failed(ex.getMessage());
        } catch (Exception ex) {
            return NodeExecutionResult.failed("Tool 节点执行失败: " + ex.getMessage());
        }
    }

    private Map<String, Object> executeRegistryTool(JsonNode config, Map<String, Object> variables) {
        if (!config.has("toolId")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Tool 节点缺少 toolId");
        }
        long toolId = config.get("toolId").asLong();
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在"));
        if (!WorkspaceContext.require().workspaceId().equals(tool.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该工具");
        }
        Map<String, Object> arguments = parseArguments(config.path("argumentsJson").asText("{}"), variables);
        String body;
        if ("HTTP".equalsIgnoreCase(tool.getType())) {
            ToolHttpConfig httpConfig = toolHttpConfigRepository.findByToolId(tool.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "HTTP 工具未配置"));
            if (config.has("bodyTemplate")) {
                ToolHttpConfig override = copyHttpConfig(httpConfig);
                override.setBodyTemplate(templateRenderer.render(config.path("bodyTemplate").asText(""), variables));
                httpConfig = override;
            }
            ToolTestResultVO result = httpToolExecutor.execute(httpConfig);
            body = result.body() == null ? "" : result.body();
        } else {
            body = toolExecutionService.execute(tool, arguments);
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("sourceType", tool.getType());
        output.put("toolId", toolId);
        output.put("toolKey", tool.getToolKey());
        output.put("arguments", arguments);
        output.put("body", body);
        return output;
    }

    private Map<String, Object> executeMcp(JsonNode config, Map<String, Object> variables) {
        if (!config.has("mcpServerId")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Tool 节点缺少 mcpServerId");
        }
        String mcpToolName = config.path("mcpToolName").asText(null);
        if (mcpToolName == null || mcpToolName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Tool 节点缺少 mcpToolName");
        }
        long mcpServerId = config.get("mcpServerId").asLong();
        McpServer server = mcpServerRepository.findById(mcpServerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "MCP Server 不存在"));
        if (!WorkspaceContext.require().workspaceId().equals(server.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该 MCP Server");
        }
        Map<String, Object> arguments = parseArguments(config.path("argumentsJson").asText("{}"), variables);
        String body = mcpToolExecutor.execute(server, mcpToolName.trim(), arguments);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("sourceType", "MCP");
        output.put("mcpServerId", mcpServerId);
        output.put("mcpToolName", mcpToolName.trim());
        output.put("arguments", arguments);
        output.put("body", body == null ? "" : body);
        return output;
    }

    private Map<String, Object> parseArguments(String template, Map<String, Object> variables) {
        String rendered = templateRenderer.render(template == null || template.isBlank() ? "{}" : template, variables);
        try {
            return objectMapper.readValue(rendered, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Tool 节点 argumentsJson 不是合法 JSON");
        }
    }

    private ToolHttpConfig copyHttpConfig(ToolHttpConfig source) {
        ToolHttpConfig copy = new ToolHttpConfig();
        copy.setToolId(source.getToolId());
        copy.setMethod(source.getMethod());
        copy.setUrl(source.getUrl());
        copy.setHeadersJson(source.getHeadersJson());
        copy.setQueryParamsJson(source.getQueryParamsJson());
        copy.setBodyType(source.getBodyType());
        copy.setBodyTemplate(source.getBodyTemplate());
        copy.setTimeoutMs(source.getTimeoutMs());
        copy.setAllowRedirect(source.getAllowRedirect());
        return copy;
    }
}
