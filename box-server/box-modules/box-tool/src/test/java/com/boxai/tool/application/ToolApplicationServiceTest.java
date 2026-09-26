package com.boxai.tool.application;

import com.boxai.common.constant.AuditActions;
import com.boxai.common.constant.AuditResourceTypes;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.AgentToolRepository;
import com.boxai.domain.crypto.SecretCipher;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolDatabaseConfigRepository;
import com.boxai.domain.tool.ToolFunctionConfig;
import com.boxai.domain.tool.ToolFunctionConfigRepository;
import com.boxai.domain.tool.ToolHttpConfigRepository;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.guard.ResourceDeleteGuard;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.security.ratelimit.RateLimitService;
import com.boxai.domain.tool.ToolDatabaseConfig;
import com.boxai.tool.api.CreateToolRequest;
import com.boxai.tool.api.DatabaseToolConfigRequest;
import com.boxai.tool.api.FunctionToolConfigRequest;
import com.boxai.tool.api.HttpToolConfigRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolApplicationServiceTest {

    @Mock
    private ToolRepository toolRepository;
    @Mock
    private ToolHttpConfigRepository toolHttpConfigRepository;
    @Mock
    private ToolDatabaseConfigRepository toolDatabaseConfigRepository;
    @Mock
    private ToolFunctionConfigRepository toolFunctionConfigRepository;
    @Mock
    private ToolExecutionService toolExecutionService;
    @Mock
    private AgentToolRepository agentToolRepository;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private SecretCipher secretCipher;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ResourceDeleteGuard resourceDeleteGuard;
    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private ToolApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createRejectsMcpType() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(
                new CreateToolRequest("mcp", "mcp_key", null, "MCP", null, null, null, null, null)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(toolFunctionConfigRepository, never()).save(any());
    }

    @Test
    void createPersistsFunctionTool() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        doSaveTool(12L);
        when(toolHttpConfigRepository.findByToolId(12L)).thenReturn(Optional.empty());
        when(toolDatabaseConfigRepository.findByToolId(12L)).thenReturn(Optional.empty());
        when(toolFunctionConfigRepository.findByToolId(12L)).thenReturn(Optional.empty());

        var vo = service.create(new CreateToolRequest(
                "  Echo  ",
                " echo_tool ",
                "  ",
                "FUNCTION",
                null,
                null,
                null,
                null,
                new FunctionToolConfigRequest("echo", "return input", "javascript", null, null)));

        verify(workspacePermissionService).requirePermission(PermissionCodes.TOOL_CREATE);
        ArgumentCaptor<Tool> toolCaptor = ArgumentCaptor.forClass(Tool.class);
        verify(toolRepository).save(toolCaptor.capture());
        assertEquals("Echo", toolCaptor.getValue().getName());
        assertEquals("echo_tool", toolCaptor.getValue().getToolKey());
        ArgumentCaptor<ToolFunctionConfig> fnCaptor = ArgumentCaptor.forClass(ToolFunctionConfig.class);
        verify(toolFunctionConfigRepository).save(fnCaptor.capture());
        assertEquals("echo", fnCaptor.getValue().getFunctionName());
        assertEquals("JAVASCRIPT", fnCaptor.getValue().getRuntime());
        assertEquals(5000, fnCaptor.getValue().getTimeoutMs());
        verify(auditLogService).recordSuccess(AuditActions.TOOL_CREATE, AuditResourceTypes.TOOL, 12L, "Echo", "FUNCTION");
        assertEquals(12L, vo.id());
    }

    @Test
    void deleteRejectsOtherWorkspace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Tool tool = new Tool();
        tool.setId(12L);
        tool.setWorkspaceId(99L);
        when(toolRepository.findById(12L)).thenReturn(Optional.of(tool));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(12L));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
        verify(toolRepository, never()).delete(anyLong());
    }

    @Test
    void deleteClearsConfigsWhenGuardAllows() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Tool tool = new Tool();
        tool.setId(12L);
        tool.setWorkspaceId(7L);
        tool.setName("Echo");
        tool.setType("FUNCTION");
        when(toolRepository.findById(12L)).thenReturn(Optional.of(tool));

        service.delete(12L);

        verify(resourceDeleteGuard).assertToolDeletable(tool);
        verify(toolHttpConfigRepository).deleteByToolId(12L);
        verify(toolDatabaseConfigRepository).deleteByToolId(12L);
        verify(toolFunctionConfigRepository).deleteByToolId(12L);
        verify(toolRepository).delete(12L);
        verify(auditLogService).recordSuccess(AuditActions.TOOL_DELETE, AuditResourceTypes.TOOL, 12L, "Echo", "FUNCTION");
    }

    @Test
    void createRejectsHttpToolWithoutConfig() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        doSaveTool(12L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(
                new CreateToolRequest("Http", "http_tool", null, "HTTP", null, null, null, null, null)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(toolHttpConfigRepository, never()).save(any());
    }

    @Test
    void createRejectsLocalHttpUrl() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        doSaveTool(12L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(
                new CreateToolRequest("Http", "http_tool", null, "HTTP", null, null,
                        new HttpToolConfigRequest("get", "http://127.0.0.1/api", null, null, null, null, null, null),
                        null, null)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(toolHttpConfigRepository, never()).save(any());
    }

    @Test
    void createPersistsDatabaseToolAndEncryptsPassword() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        doSaveTool(12L);
        when(secretCipher.encrypt("secret")).thenReturn("cipher");
        when(toolHttpConfigRepository.findByToolId(12L)).thenReturn(Optional.empty());
        when(toolDatabaseConfigRepository.findByToolId(12L)).thenReturn(Optional.empty());
        when(toolFunctionConfigRepository.findByToolId(12L)).thenReturn(Optional.empty());

        var vo = service.create(new CreateToolRequest(
                "Orders",
                "orders_db",
                null,
                "DATABASE",
                null,
                null,
                null,
                new DatabaseToolConfigRequest("mysql", "db.internal", null, "box", "app", "secret", null, null, null),
                null));

        ArgumentCaptor<ToolDatabaseConfig> captor = ArgumentCaptor.forClass(ToolDatabaseConfig.class);
        verify(toolDatabaseConfigRepository).save(captor.capture());
        assertEquals("MYSQL", captor.getValue().getDatabaseType());
        assertEquals(3306, captor.getValue().getPort());
        assertEquals("cipher", captor.getValue().getPasswordCiphertext());
        assertEquals(100, captor.getValue().getMaxRows());
        assertEquals(12L, vo.id());
    }

    @Test
    void createRejectsDatabaseToolWithoutPassword() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        doSaveTool(12L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(
                new CreateToolRequest("Orders", "orders_db", null, "DATABASE", null, null, null,
                        new DatabaseToolConfigRequest("mysql", "db.internal", 3306, "box", "app", "  ", null, 20, 5000),
                        null)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(toolDatabaseConfigRepository, never()).save(any());
    }

    private void doSaveTool(Long id) {
        org.mockito.Mockito.doAnswer(invocation -> {
            Tool tool = invocation.getArgument(0);
            tool.setId(id);
            return tool;
        }).when(toolRepository).save(any(Tool.class));
    }
}
