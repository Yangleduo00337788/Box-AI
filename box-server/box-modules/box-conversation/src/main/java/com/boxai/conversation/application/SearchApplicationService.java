package com.boxai.conversation.application;

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
import com.boxai.common.constant.PermissionCodes;
import com.boxai.conversation.api.SearchResultVO;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchApplicationService {

    private static final int SEARCH_LIMIT = 12;

    private final AgentRepository agentRepository;
    private final ConversationRepository conversationRepository;
    private final WorkflowRepository workflowRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ToolRepository toolRepository;
    private final PluginCatalogRepository pluginCatalogRepository;
    private final WorkspacePermissionService workspacePermissionService;

    public SearchApplicationService(AgentRepository agentRepository,
                                    ConversationRepository conversationRepository,
                                    WorkflowRepository workflowRepository,
                                    KnowledgeBaseRepository knowledgeBaseRepository,
                                    ToolRepository toolRepository,
                                    PluginCatalogRepository pluginCatalogRepository,
                                    WorkspacePermissionService workspacePermissionService) {
        this.agentRepository = agentRepository;
        this.conversationRepository = conversationRepository;
        this.workflowRepository = workflowRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.toolRepository = toolRepository;
        this.pluginCatalogRepository = pluginCatalogRepository;
        this.workspacePermissionService = workspacePermissionService;
    }

    public List<SearchResultVO> search(String keyword) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        String q = keyword.trim();

        int perTypeLimit = 3;
        List<Agent> agents = agentRepository.searchByName(workspaceId, q, perTypeLimit);
        List<Conversation> conversations = conversationRepository.searchByTitle(workspaceId, userId, q, perTypeLimit);
        List<Workflow> workflows = workflowRepository.searchByName(workspaceId, q, perTypeLimit);
        List<KnowledgeBase> knowledgeBases = knowledgeBaseRepository.searchByName(workspaceId, q, perTypeLimit);
        List<Tool> tools = toolRepository.searchByName(workspaceId, q, perTypeLimit);
        List<PluginCatalog> plugins = pluginCatalogRepository.searchByTitle(workspaceId, q, perTypeLimit);
        Map<Long, String> agentNames = agentRepository.listByWorkspace(workspaceId).stream()
                .collect(Collectors.toMap(Agent::getId, Agent::getName, (a, b) -> a));

        List<SearchResultVO> results = new ArrayList<>();
        for (Agent agent : agents) {
            results.add(new SearchResultVO(
                    "AGENT",
                    agent.getId(),
                    agent.getName(),
                    "打开对话",
                    "/chat"));
        }
        for (Conversation conversation : conversations) {
            String agentName = agentNames.getOrDefault(conversation.getAgentId(), "");
            String title = conversation.getTitle();
            if (title == null || title.isBlank()) {
                title = agentName.isBlank() ? "会话 #" + conversation.getId() : agentName;
            }
            results.add(new SearchResultVO(
                    "CONVERSATION",
                    conversation.getId(),
                    title,
                    agentName,
                    "/chat/" + conversation.getId()));
        }
        for (Workflow workflow : workflows) {
            results.add(new SearchResultVO(
                    "WORKFLOW",
                    workflow.getId(),
                    workflow.getName(),
                    workflow.getDescription(),
                    "/workflows/" + workflow.getId() + "/editor"));
        }
        for (KnowledgeBase knowledgeBase : knowledgeBases) {
            results.add(new SearchResultVO(
                    "KNOWLEDGE",
                    knowledgeBase.getId(),
                    knowledgeBase.getName(),
                    knowledgeBase.getDescription(),
                    "/plugin-market?mine=knowledge"));
        }
        for (Tool tool : tools) {
            results.add(new SearchResultVO(
                    "TOOL",
                    tool.getId(),
                    tool.getName(),
                    tool.getDescription(),
                    "/plugin-market?mine=tools"));
        }
        for (PluginCatalog plugin : plugins) {
            results.add(new SearchResultVO(
                    "PLUGIN",
                    plugin.getId(),
                    plugin.getTitle(),
                    plugin.getDescription(),
                    "/plugin-market?category=" + plugin.getCategory()));
        }
        return results.stream().limit(SEARCH_LIMIT).toList();
    }
}
