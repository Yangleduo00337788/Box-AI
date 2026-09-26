package com.boxai.agent.application;

import com.boxai.agent.api.market.UpdateMarketRolloutRequest;
import com.boxai.agent.api.plugin.CreatePluginCatalogRequest;
import com.boxai.agent.api.plugin.UpdatePluginCatalogRequest;
import com.boxai.agent.api.plugin.UpdatePluginReviewRequest;
import com.boxai.common.constant.MarketReviewStatuses;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.domain.plugin.WorkspacePluginInstallRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPluginCatalogApplicationServiceTest {

    @Mock
    private PluginCatalogRepository pluginCatalogRepository;
    @Mock
    private PluginCategoryApplicationService pluginCategoryApplicationService;
    @Mock
    private WorkspacePluginInstallRepository workspacePluginInstallRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AdminPluginCatalogApplicationService service;

    @Test
    void createRejectsDuplicateCode() {
        when(pluginCatalogRepository.findByCode("echo")).thenReturn(Optional.of(new PluginCatalog()));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(
                new CreatePluginCatalogRequest("echo", "skills", "Echo", null, "{\"instructions\":\"hello\"}", 1)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(pluginCatalogRepository, never()).save(any());
    }

    @Test
    void createPersistsAdminPluginPendingReview() {
        when(pluginCatalogRepository.findByCode("helper")).thenReturn(Optional.empty());

        var vo = service.create(new CreatePluginCatalogRequest(
                "helper", "skills", " Helper ", "desc", "{\"instructions\":\"hello\"}", null));

        ArgumentCaptor<PluginCatalog> captor = ArgumentCaptor.forClass(PluginCatalog.class);
        verify(pluginCatalogRepository).save(captor.capture());
        PluginCatalog saved = captor.getValue();
        assertEquals("helper", saved.getPluginCode());
        assertEquals("ADMIN", saved.getSourceType());
        assertEquals("LISTED", saved.getStatus());
        assertEquals(MarketReviewStatuses.PENDING_REVIEW, saved.getReviewStatus());
        assertEquals(0, saved.getSortOrder());
        assertEquals("helper", vo.pluginCode());
    }

    @Test
    void updateCannotListUnapprovedPlugin() {
        PluginCatalog plugin = plugin(9L, MarketReviewStatuses.PENDING_REVIEW, "UNLISTED");
        when(pluginCatalogRepository.findById(9L)).thenReturn(Optional.of(plugin));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(
                9L, new UpdatePluginCatalogRequest(null, null, null, null, null, "listed")));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(pluginCatalogRepository, never()).update(any());
    }

    @Test
    void rejectReviewUnlistsListedPlugin() {
        PluginCatalog plugin = plugin(9L, MarketReviewStatuses.APPROVED, "LISTED");
        when(pluginCatalogRepository.findById(9L)).thenReturn(Optional.of(plugin));

        var vo = service.updateReview(9L, new UpdatePluginReviewRequest("rejected"));

        assertEquals(MarketReviewStatuses.REJECTED, vo.reviewStatus());
        assertEquals("UNLISTED", vo.status());
        verify(pluginCatalogRepository).update(plugin);
    }

    @Test
    void tenantRolloutRequiresTenantIds() {
        PluginCatalog plugin = plugin(9L, MarketReviewStatuses.APPROVED, "LISTED");
        when(pluginCatalogRepository.findById(9L)).thenReturn(Optional.of(plugin));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateRollout(
                9L, new UpdateMarketRolloutRequest("TENANT", "  ", 50)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void globalRolloutClearsTenantIds() {
        PluginCatalog plugin = plugin(9L, MarketReviewStatuses.APPROVED, "LISTED");
        plugin.setTenantIdsJson("[1]");
        when(pluginCatalogRepository.findById(9L)).thenReturn(Optional.of(plugin));

        var vo = service.updateRollout(9L, new UpdateMarketRolloutRequest("global", "1,2", 80));

        assertEquals("GLOBAL", vo.visibility());
        assertNull(vo.tenantIdsJson());
        assertEquals(80, vo.rolloutPercent());
    }

    @Test
    void deleteRejectsInstalledPlugin() {
        when(pluginCatalogRepository.findById(9L)).thenReturn(Optional.of(plugin(9L, MarketReviewStatuses.APPROVED, "LISTED")));
        when(workspacePluginInstallRepository.countByPluginId(9L)).thenReturn(2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(9L));
        assertEquals(ErrorCode.CONFLICT, ex.getCode());
        verify(pluginCatalogRepository, never()).delete(anyLong());
    }

    @Test
    void listFiltersCategoryCaseInsensitive() {
        PluginCatalog skills = plugin(1L, MarketReviewStatuses.APPROVED, "LISTED");
        skills.setCategory("skills");
        PluginCatalog tools = plugin(2L, MarketReviewStatuses.APPROVED, "LISTED");
        tools.setCategory("tools");
        when(pluginCatalogRepository.listAllForAdmin()).thenReturn(List.of(skills, tools));

        assertEquals(List.of(1L), service.list("SKILLS").stream().map(item -> item.id()).toList());
    }

    private static PluginCatalog plugin(Long id, String reviewStatus, String status) {
        PluginCatalog plugin = new PluginCatalog();
        plugin.setId(id);
        plugin.setPluginCode("p-" + id);
        plugin.setCategory("skills");
        plugin.setTitle("Plugin");
        plugin.setReviewStatus(reviewStatus);
        plugin.setStatus(status);
        plugin.setSourceType("ADMIN");
        plugin.setVisibility("GLOBAL");
        plugin.setRolloutPercent(100);
        plugin.setSortOrder(0);
        plugin.setInstallCount(0);
        return plugin;
    }
}
