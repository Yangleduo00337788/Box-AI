package com.boxai.tool.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.mcp.McpServer;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.notification.NotificationPublisher;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tool.api.CreateMcpServerRequest;
import com.boxai.tool.mcp.McpProtocolClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpServerApplicationServiceTest {

    @Mock
    private McpServerRepository mcpServerRepository;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private McpProtocolClient mcpProtocolClient;
    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private McpServerApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createRejectsLocalEndpoint() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(
                new CreateMcpServerRequest("Local", "local_mcp", null, "http://127.0.0.1:3000/sse", "SSE", "NONE", null)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(mcpServerRepository, never()).save(any());
    }

    @Test
    void deleteRejectsOtherWorkspace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        McpServer server = server(5L, 99L);
        when(mcpServerRepository.findById(5L)).thenReturn(Optional.of(server));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(5L));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
        verify(mcpServerRepository, never()).delete(anyLong());
    }

    @Test
    void syncNotifiesWhenProtocolFails() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        McpServer server = server(5L, 7L);
        server.setName("Docs");
        when(mcpServerRepository.findById(5L)).thenReturn(Optional.of(server));
        when(mcpProtocolClient.listTools(server)).thenThrow(new BusinessException(ErrorCode.EXECUTION_FAILED, "timeout"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.sync(5L));
        assertEquals(ErrorCode.EXECUTION_FAILED, ex.getCode());
        verify(workspacePermissionService).requirePermission(PermissionCodes.TOOL_EXECUTE);
        verify(notificationPublisher).publish(eq(3L), eq(7L), eq("MCP 同步失败"), any(), eq("MCP"), eq("/mcp"));
        verify(mcpServerRepository, never()).update(any());
    }

    @Test
    void createRejectsNonHttpEndpoint() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(
                new CreateMcpServerRequest("Remote", "remote_mcp", null, "ftp://example.com/mcp", "SSE", "NONE", null)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(mcpServerRepository, never()).save(any());
    }

    @Test
    void syncPersistsToolCatalog() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        McpServer server = server(5L, 7L);
        server.setName("Docs");
        when(mcpServerRepository.findById(5L)).thenReturn(Optional.of(server));
        var tools = List.of(new McpProtocolClient.McpToolDescriptor("search", "Find docs"));
        when(mcpProtocolClient.listTools(server)).thenReturn(tools);
        when(mcpProtocolClient.catalogJson(tools)).thenReturn("[{\"name\":\"search\"}]");

        var vo = service.sync(5L);

        verify(mcpServerRepository).update(server);
        assertEquals("[{\"name\":\"search\"}]", vo.toolCatalogJson());
        assertEquals("[{\"name\":\"search\"}]", server.getToolCatalogJson());
    }

    private static McpServer server(Long id, Long workspaceId) {
        McpServer server = new McpServer();
        server.setId(id);
        server.setWorkspaceId(workspaceId);
        return server;
    }
}
