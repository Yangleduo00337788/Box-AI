package com.boxai.agent.application;

import com.boxai.agent.api.plugin.PluginCategoryVO;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.domain.plugin.PluginCategory;
import com.boxai.domain.plugin.PluginCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginCategoryApplicationServiceTest {

    @Mock
    private PluginCategoryRepository pluginCategoryRepository;
    @Mock
    private PluginCatalogRepository pluginCatalogRepository;

    @InjectMocks
    private PluginCategoryApplicationService service;

    @Test
    void requireActiveCategoryIgnoresBlankAndRejectsUnknown() {
        service.requireActiveCategory("  ");
        verify(pluginCategoryRepository, never()).existsActive(any());

        when(pluginCategoryRepository.existsActive("tools")).thenReturn(false);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.requireActiveCategory(" tools "));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void createRejectsDuplicateCode() {
        when(pluginCategoryRepository.findByCode("tools")).thenReturn(Optional.of(new PluginCategory()));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create("tools", "工具", null, 1));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(pluginCategoryRepository, never()).save(any());
    }

    @Test
    void createPersistsActiveCategory() {
        when(pluginCategoryRepository.findByCode("tools")).thenReturn(Optional.empty());

        PluginCategoryVO vo = service.create("tools", "工具", "HTTP 工具", null);

        ArgumentCaptor<PluginCategory> captor = ArgumentCaptor.forClass(PluginCategory.class);
        verify(pluginCategoryRepository).save(captor.capture());
        assertEquals("tools", captor.getValue().getCategoryCode());
        assertEquals("ACTIVE", captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getSortOrder());
        assertEquals("tools", vo.value());
        assertEquals("工具", vo.label());
    }

    @Test
    void deleteRejectsCategoryThatStillHasPlugins() {
        when(pluginCategoryRepository.findByCode("tools")).thenReturn(Optional.of(new PluginCategory()));
        when(pluginCatalogRepository.countByCategory("tools")).thenReturn(2L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete("tools"));
        assertEquals(ErrorCode.CONFLICT, ex.getCode());
        verify(pluginCategoryRepository, never()).delete(any());
    }

    @Test
    void listActiveMapsCategories() {
        PluginCategory category = new PluginCategory();
        category.setCategoryCode("mcp");
        category.setLabel("MCP");
        category.setDescription("servers");
        when(pluginCategoryRepository.listActive()).thenReturn(List.of(category));
        PluginCategoryVO vo = service.listActive().get(0);
        assertEquals("mcp", vo.value());
        assertEquals("servers", vo.desc());
    }
}
