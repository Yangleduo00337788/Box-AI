package com.boxai.agent.application;

import com.boxai.agent.chat.ConversationPluginRound;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.domain.plugin.WorkspacePluginInstall;
import com.boxai.domain.plugin.WorkspacePluginInstallRepository;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.tool.application.McpToolCatalogParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationPluginApplicationServiceTest {

    @Mock
    private PluginCatalogRepository pluginCatalogRepository;
    @Mock
    private WorkspacePluginInstallRepository workspacePluginInstallRepository;
    @Mock
    private ToolRepository toolRepository;
    @Mock
    private McpServerRepository mcpServerRepository;
    @Mock
    private McpToolCatalogParser mcpToolCatalogParser;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private PluginCatalogAssetApplicationService pluginCatalogAssetApplicationService;

    @InjectMocks
    private ConversationPluginApplicationService service;

    @Test
    void resolveReturnsEmptyWhenNoPlugins() {
        ConversationPluginRound round = service.resolve(7L, List.of());
        assertTrue(round.extraTools().isEmpty());
        assertEquals(null, round.skillPromptBlock());
    }

    @Test
    void resolveRejectsTooManyPlugins() {
        List<Long> ids = LongStream.rangeClosed(1, 9).boxed().toList();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.resolve(7L, ids));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void resolveRejectsUninstalledPlugin() {
        when(workspacePluginInstallRepository.findByWorkspaceAndPlugin(7L, 3L)).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.resolve(7L, List.of(3L)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void resolveRejectsKnowledgeCategory() {
        WorkspacePluginInstall install = new WorkspacePluginInstall();
        when(workspacePluginInstallRepository.findByWorkspaceAndPlugin(7L, 3L)).thenReturn(Optional.of(install));
        PluginCatalog plugin = new PluginCatalog();
        plugin.setCategory("knowledge");
        when(pluginCatalogRepository.findById(3L)).thenReturn(Optional.of(plugin));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.resolve(7L, List.of(3L)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void resolveAddsInstalledTool() {
        WorkspacePluginInstall install = new WorkspacePluginInstall();
        install.setResourceType("tool");
        install.setResourceId(12L);
        when(workspacePluginInstallRepository.findByWorkspaceAndPlugin(7L, 3L)).thenReturn(Optional.of(install));
        PluginCatalog plugin = new PluginCatalog();
        plugin.setCategory("tools");
        when(pluginCatalogRepository.findById(3L)).thenReturn(Optional.of(plugin));
        Tool tool = new Tool();
        tool.setId(12L);
        tool.setToolKey("echo");
        tool.setName("Echo");
        tool.setType("FUNCTION");
        tool.setStatus(1);
        when(toolRepository.findById(12L)).thenReturn(Optional.of(tool));

        ConversationPluginRound round = service.resolve(7L, List.of(3L));

        assertEquals(1, round.extraTools().size());
        assertEquals("echo", round.extraTools().get(0).toolKey());
        assertEquals("FUNCTION", round.extraTools().get(0).type());
    }
}
