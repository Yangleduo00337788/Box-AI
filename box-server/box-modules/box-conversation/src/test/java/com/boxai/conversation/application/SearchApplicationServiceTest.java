package com.boxai.conversation.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.conversation.api.SearchResultVO;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.conversation.Conversation;
import com.boxai.domain.conversation.ConversationRepository;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchApplicationServiceTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private WorkflowRepository workflowRepository;
    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private ToolRepository toolRepository;
    @Mock
    private PluginCatalogRepository pluginCatalogRepository;
    @Mock
    private WorkspacePermissionService workspacePermissionService;

    @InjectMocks
    private SearchApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void searchReturnsEmptyForBlankKeyword() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));

        assertTrue(service.search("  ").isEmpty());
        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_READ);
        verify(agentRepository, never()).searchByName(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void searchAggregatesTypesAndFillsUntitledConversation() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = new Agent();
        agent.setId(21L);
        agent.setName("Helper");
        when(agentRepository.searchByName(7L, "help", 3)).thenReturn(List.of(agent));
        when(agentRepository.listByWorkspace(7L)).thenReturn(List.of(agent));
        Conversation conversation = new Conversation();
        conversation.setId(5L);
        conversation.setAgentId(21L);
        conversation.setTitle("  ");
        when(conversationRepository.searchByTitle(7L, 3L, "help", 3)).thenReturn(List.of(conversation));
        Workflow workflow = new Workflow();
        workflow.setId(8L);
        workflow.setName("Flow");
        workflow.setDescription("desc");
        when(workflowRepository.searchByName(7L, "help", 3)).thenReturn(List.of(workflow));
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(4L);
        kb.setName("Docs");
        when(knowledgeBaseRepository.searchByName(7L, "help", 3)).thenReturn(List.of(kb));
        Tool tool = new Tool();
        tool.setId(6L);
        tool.setName("HTTP");
        when(toolRepository.searchByName(7L, "help", 3)).thenReturn(List.of(tool));
        PluginCatalog plugin = new PluginCatalog();
        plugin.setId(9L);
        plugin.setTitle("Skill");
        plugin.setCategory("skills");
        when(pluginCatalogRepository.searchByTitle(7L, "help", 3)).thenReturn(List.of(plugin));

        List<SearchResultVO> results = service.search(" help ");

        assertEquals(6, results.size());
        assertEquals("AGENT", results.get(0).type());
        assertEquals("/chat", results.get(0).route());
        assertEquals("CONVERSATION", results.get(1).type());
        assertEquals("Helper", results.get(1).title());
        assertEquals("/chat/5", results.get(1).route());
        assertEquals("WORKFLOW", results.get(2).type());
        assertEquals("/workflows/8/editor", results.get(2).route());
        assertEquals("KNOWLEDGE", results.get(3).type());
        assertEquals("TOOL", results.get(4).type());
        assertEquals("PLUGIN", results.get(5).type());
        assertEquals("/plugin-market?category=skills", results.get(5).route());
    }
}
