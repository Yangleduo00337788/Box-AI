package com.boxai.agent.application;

import com.boxai.common.constant.MarketReviewStatuses;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.domain.plugin.WorkspacePluginInstall;
import com.boxai.domain.plugin.WorkspacePluginInstallRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginMarketApplicationServiceTest {

    @Mock
    private PluginCatalogRepository pluginCatalogRepository;
    @Mock
    private WorkspacePluginInstallRepository workspacePluginInstallRepository;
    @Mock
    private PluginCategoryApplicationService pluginCategoryApplicationService;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private PluginInstallProvisioner pluginInstallProvisioner;
    @Mock
    private MarketRolloutResolver marketRolloutResolver;

    @InjectMocks
    private PluginMarketApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void listHidesAdminPluginsOutsideRollout() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        PluginCatalog listed = plugin(1L, "ADMIN", "LISTED", MarketReviewStatuses.APPROVED);
        PluginCatalog hidden = plugin(2L, "ADMIN", "LISTED", MarketReviewStatuses.APPROVED);
        PluginCatalog userOwned = plugin(3L, "USER", "LISTED", null);
        when(pluginCatalogRepository.listByCategory(7L, "tools")).thenReturn(List.of(listed, hidden, userOwned));
        when(workspacePluginInstallRepository.listByWorkspace(7L)).thenReturn(List.of());
        when(marketRolloutResolver.isVisible(1L, listed.getVisibility(), listed.getTenantIdsJson(), listed.getRolloutPercent()))
                .thenReturn(true);
        when(marketRolloutResolver.isVisible(2L, hidden.getVisibility(), hidden.getTenantIdsJson(), hidden.getRolloutPercent()))
                .thenReturn(false);

        var vos = service.list("tools");

        verify(workspacePermissionService).requirePermission(PermissionCodes.TOOL_EXECUTE);
        verify(pluginCategoryApplicationService).requireActiveCategory("tools");
        assertEquals(List.of(1L, 3L), vos.stream().map(item -> item.id()).toList());
        assertFalse(vos.get(0).installed());
    }

    @Test
    void installIsIdempotentWhenAlreadyInstalled() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        PluginCatalog plugin = plugin(1L, "ADMIN", "LISTED", MarketReviewStatuses.APPROVED);
        when(pluginCatalogRepository.findById(1L)).thenReturn(Optional.of(plugin));
        when(marketRolloutResolver.isVisible(anyLong(), any(), any(), any())).thenReturn(true);
        when(workspacePluginInstallRepository.findByWorkspaceAndPlugin(7L, 1L))
                .thenReturn(Optional.of(new WorkspacePluginInstall()));

        service.install(1L);

        verify(pluginInstallProvisioner, never()).provision(any(), anyLong(), anyLong());
        verify(pluginCatalogRepository, never()).incrementInstallCount(anyLong());
    }

    @Test
    void installProvisionsListedPlugin() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        PluginCatalog plugin = plugin(1L, "ADMIN", "LISTED", MarketReviewStatuses.APPROVED);
        when(pluginCatalogRepository.findById(1L)).thenReturn(Optional.of(plugin));
        when(marketRolloutResolver.isVisible(anyLong(), any(), any(), any())).thenReturn(true);
        when(workspacePluginInstallRepository.findByWorkspaceAndPlugin(7L, 1L)).thenReturn(Optional.empty());
        when(pluginInstallProvisioner.provision(plugin, 7L, 3L))
                .thenReturn(new PluginInstallProvisioner.ProvisionResult("tool", 44L));

        service.install(1L);

        ArgumentCaptor<WorkspacePluginInstall> captor = ArgumentCaptor.forClass(WorkspacePluginInstall.class);
        verify(workspacePluginInstallRepository).save(captor.capture());
        assertEquals("tool", captor.getValue().getResourceType());
        assertEquals(44L, captor.getValue().getResourceId());
        verify(pluginCatalogRepository).incrementInstallCount(1L);
        verify(workspacePermissionService).requirePermission(PermissionCodes.TOOL_CREATE);
    }

    @Test
    void installRejectsUnlistedPlugin() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        PluginCatalog plugin = plugin(1L, "ADMIN", "DRAFT", MarketReviewStatuses.PENDING_REVIEW);
        when(pluginCatalogRepository.findById(1L)).thenReturn(Optional.of(plugin));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.install(1L));
        assertEquals(ErrorCode.PLUGIN_NOT_FOUND, ex.getCode());
    }

    @Test
    void uninstallDeprovisionsAndDecrementsCount() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        PluginCatalog plugin = plugin(1L, "ADMIN", "LISTED", MarketReviewStatuses.APPROVED);
        when(pluginCatalogRepository.findById(1L)).thenReturn(Optional.of(plugin));
        when(marketRolloutResolver.isVisible(anyLong(), any(), any(), any())).thenReturn(true);
        WorkspacePluginInstall install = new WorkspacePluginInstall();
        install.setId(90L);
        when(workspacePluginInstallRepository.findByWorkspaceAndPlugin(7L, 1L)).thenReturn(Optional.of(install));

        service.uninstall(1L);

        verify(pluginInstallProvisioner).deprovision(install);
        verify(workspacePluginInstallRepository).delete(90L);
        verify(pluginCatalogRepository).decrementInstallCount(1L);
    }

    private static PluginCatalog plugin(Long id, String source, String status, String review) {
        PluginCatalog plugin = new PluginCatalog();
        plugin.setId(id);
        plugin.setPluginCode("p-" + id);
        plugin.setCategory("tools");
        plugin.setTitle("Plugin " + id);
        plugin.setSourceType(source);
        plugin.setStatus(status);
        plugin.setReviewStatus(review);
        plugin.setSubmittedWorkspaceId(7L);
        plugin.setInstallCount(0);
        return plugin;
    }
}
