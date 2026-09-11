package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.mcp.McpServer;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.tool.api.CreateMcpServerRequest;
import com.boxai.tool.api.McpServerVO;
import com.boxai.tool.api.UpdateMcpServerRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class McpServerApplicationService {

    private final McpServerRepository mcpServerRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public McpServerApplicationService(McpServerRepository mcpServerRepository) {
        this.mcpServerRepository = mcpServerRepository;
    }

    public List<McpServerVO> list() {
        return mcpServerRepository.listByWorkspace(workspaceId()).stream().map(this::toVO).toList();
    }

    public McpServerVO detail(Long id) {
        return toVO(requireServer(id));
    }

    @Transactional
    public McpServerVO create(CreateMcpServerRequest request) {
        Long userId = WorkspaceContext.require().userId();
        McpServer server = new McpServer();
        server.setWorkspaceId(workspaceId());
        server.setName(request.name().trim());
        server.setServerKey(request.serverKey().trim());
        server.setDescription(trimToNull(request.description()));
        server.setTransportType(request.transportType() == null ? "SSE" : request.transportType().trim().toUpperCase());
        server.setEndpointUrl(request.endpointUrl().trim());
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
        McpServer server = requireServer(id);
        server.setName(request.name().trim());
        server.setDescription(trimToNull(request.description()));
        server.setEndpointUrl(request.endpointUrl().trim());
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
        requireServer(id);
        mcpServerRepository.delete(id);
    }

    @Transactional
    public McpServerVO sync(Long id) {
        McpServer server = requireServer(id);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server.getEndpointUrl()))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String catalog = response.statusCode() >= 200 && response.statusCode() < 500
                    ? "[{\"name\":\"discovered\",\"description\":\"Endpoint reachable\"}]"
                    : "[]";
            server.setToolCatalogJson(catalog);
            server.setLastSyncAt(LocalDateTime.now());
            mcpServerRepository.update(server);
            return toVO(server);
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
}
