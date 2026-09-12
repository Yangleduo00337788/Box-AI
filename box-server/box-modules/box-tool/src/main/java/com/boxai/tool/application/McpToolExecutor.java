package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.mcp.McpServer;
import com.boxai.tool.mcp.McpProtocolClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class McpToolExecutor {

    private final McpProtocolClient mcpProtocolClient;

    public McpToolExecutor(McpProtocolClient mcpProtocolClient) {
        this.mcpProtocolClient = mcpProtocolClient;
    }

    public String execute(McpServer server, String toolName) {
        return execute(server, toolName, Map.of());
    }

    public String execute(McpServer server, String toolName, Map<String, Object> arguments) {
        if (server == null || server.getEndpointUrl() == null || server.getEndpointUrl().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "MCP Server 未配置端点");
        }
        if (toolName == null || toolName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "MCP 工具名称不能为空");
        }
        try {
            return mcpProtocolClient.callTool(server, toolName, arguments);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "MCP 工具调用失败: " + e.getMessage());
        }
    }
}
