package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.SsrfGuard;
import com.boxai.domain.mcp.McpServer;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tool.api.CreateMcpServerRequest;
import com.boxai.tool.api.McpServerVO;
import com.boxai.tool.api.UpdateMcpServerRequest;
import com.boxai.tool.mcp.McpProtocolClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class McpServerApplicationService {

    private final McpServerRepository mcpServerRepository;
    private final WorkspacePermissionService workspacePermissionService;
    private final McpProtocolClient mcpProtocolClient;

    public McpServerApplicationService(McpServerRepository mcpServerRepository,
                                       WorkspacePermissionService workspacePermissionService,
                                       McpProtocolClient mcpProtocolClient) {
        this.mcpServerRepository = mcpServerRepository;
        this.workspacePermissionService = workspacePermissionService;
        this.mcpProtocolClient = mcpProtocolClient;
    }

    public List<McpServerVO> list() {
        workspacePermissionService.requirePermission("tool:execute");
        return mcpServerRepository.listByWorkspace(workspaceId()).stream().map(this::toVO).toList();
    }

    public McpServerVO detail(Long id) {
        workspacePermissionService.requirePermission("tool:execute");
        return toVO(requireServer(id));
    }

    @Transactional
    public McpServerVO create(CreateMcpServerRequest request) {
        workspacePermissionService.requirePermission("tool:create");
        Long userId = WorkspaceContext.require().userId();
        McpServer server = new McpServer();
        server.setWorkspaceId(workspaceId());
        server.setName(request.name().trim());
        server.setServerKey(request.serverKey().trim());
        server.setDescription(trimToNull(request.description()));
        server.setTransportType(request.transportType() == null ? "SSE" : request.transportType().trim().toUpperCase());
        server.setEndpointUrl(validateEndpoint(request.endpointUrl()));
        server.setAuthType(request.authType() == null ? "NONE" : request.authType().trim().toUpperCase());
        server.setAuthConfigJson(trimToNull(request.authConfigJson()));
        server.setToolCatalogJson("[]");
        server.setStatus(1);
        server.setCreatedBy(userId);
        mcpServerRepository.save(server);
        return toVO(server);
    }

    @Transactional
    public McpServerVO update(Long id, UpdateMcpServerRequest request) {
        workspacePermissionService.requirePermission("tool:create");
        McpServer server = requireServer(id);
        server.setName(request.name().trim());
        server.setDescription(trimToNull(request.description()));
        server.setEndpointUrl(validateEndpoint(request.endpointUrl()));
        server.setTransportType(request.transportType() == null ? server.getTransportType() : request.transportType().trim().toUpperCase());
        server.setAuthType(request.authType() == null ? server.getAuthType() : request.authType().trim().toUpperCase());
        server.setAuthConfigJson(trimToNull(request.authConfigJson()));
        if (request.status() != null) {
            server.setStatus(request.status());
        }
        mcpServerRepository.update(server);
        return toVO(server);
    }

    @Transactional
    public void delete(Long id) {
        workspacePermissionService.requirePermission("tool:create");
        requireServer(id);
        mcpServerRepository.delete(id);
    }

    @Transactional
    public McpServerVO sync(Long id) {
        workspacePermissionService.requirePermission("tool:execute");
        McpServer server = requireServer(id);
        try {
            List<McpProtocolClient.McpToolDescriptor> tools = mcpProtocolClient.listTools(server);
            server.setToolCatalogJson(mcpProtocolClient.catalogJson(tools));
            server.setLastSyncAt(LocalDateTime.now());
            mcpServerRepository.update(server);
            return toVO(server);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "MCP 同步失败: " + e.getMessage());
        }
    }

    private McpServer requireServer(Long id) {
        McpServer server = mcpServerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "MCP Server 不存在"));
        if (!workspaceId().equals(server.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该 MCP Server");
        }
        return server;
    }

    private McpServerVO toVO(McpServer server) {
        return new McpServerVO(
                server.getId(),
                server.getName(),
                server.getServerKey(),
                server.getDescription(),
                server.getTransportType(),
                server.getEndpointUrl(),
                server.getAuthType(),
                server.getToolCatalogJson(),
                server.getStatus(),
                server.getLastSyncAt(),
                server.getCreatedAt(),
                server.getUpdatedAt());
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String validateEndpoint(String endpointUrl) {
        return SsrfGuard.validateHttpUrl(endpointUrl).toString();
    }
}
