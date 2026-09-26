package com.boxai.agent.application;

import com.boxai.agent.api.plugin.CreateWorkspacePluginRequest;
import com.boxai.agent.api.plugin.PluginCatalogVO;
import com.boxai.common.constant.MarketReviewStatuses;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspacePluginCatalogApplicationServiceTest {

    @Mock
    private PluginCatalogRepository pluginCatalogRepository;
    @Mock
    private PluginCategoryApplicationService pluginCategoryApplicationService;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private PluginMarketApplicationService pluginMarketApplicationService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WorkspacePluginCatalogApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createRejectsInvalidManifest() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(
                new CreateWorkspacePluginRequest("tools", "Echo", null, "{")));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(pluginCatalogRepository, never()).save(any());
    }

    @Test
    void createRejectsToolManifestWithoutUrl() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(
                new CreateWorkspacePluginRequest("tools", "Echo", null, "{\"method\":\"GET\"}")));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(pluginCatalogRepository, never()).save(any());
    }

    @Test
    void createSavesUserPluginAndInstallsIt() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        org.mockito.Mockito.doAnswer(invocation -> {
            PluginCatalog plugin = invocation.getArgument(0);
            plugin.setId(44L);
            return plugin;
        }).when(pluginCatalogRepository).save(any(PluginCatalog.class));
        PluginCatalogVO vo = new PluginCatalogVO(
                44L, "ws-7-abc", "skills", "Helper", null, 1, true, null, null, null, "USER", "LISTED",
                MarketReviewStatuses.APPROVED);
        when(pluginMarketApplicationService.findVo(44L)).thenReturn(vo);

        var result = service.create(new CreateWorkspacePluginRequest(
                "skills", " Helper ", "desc", "{\"instructions\":\"hello\"}"));

        verify(workspacePermissionService).requirePermission(PermissionCodes.TOOL_CREATE);
        verify(pluginCategoryApplicationService).requireActiveCategory("skills");
        ArgumentCaptor<PluginCatalog> captor = ArgumentCaptor.forClass(PluginCatalog.class);
        verify(pluginCatalogRepository).save(captor.capture());
        PluginCatalog saved = captor.getValue();
        assertTrue(saved.getPluginCode().startsWith("ws-7-"));
        assertEquals("USER", saved.getSourceType());
        assertEquals("LISTED", saved.getStatus());
        assertEquals(MarketReviewStatuses.APPROVED, saved.getReviewStatus());
        assertEquals(7L, saved.getSubmittedWorkspaceId());
        verify(pluginMarketApplicationService).install(44L);
        assertEquals(44L, result.id());
        assertTrue(result.installed());
    }
}
