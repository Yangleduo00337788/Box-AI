package com.boxai.conversation.application;

import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.conversation.Conversation;
import com.boxai.domain.conversation.ConversationRepository;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.conversation.api.SearchResultVO;
import com.boxai.security.context.WorkspaceContext;
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
    private final PluginCatalogRepository pluginCatalogRepository;

    public SearchApplicationService(AgentRepository agentRepository,
                                    ConversationRepository conversationRepository,
                                    PluginCatalogRepository pluginCatalogRepository) {
        this.agentRepository = agentRepository;
        this.conversationRepository = conversationRepository;
        this.pluginCatalogRepository = pluginCatalogRepository;
    }

    public List<SearchResultVO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        String q = keyword.trim();

        int perTypeLimit = 4;
        List<Agent> agents = agentRepository.searchByName(workspaceId, q, perTypeLimit);
        List<Conversation> conversations = conversationRepository.searchByTitle(workspaceId, userId, q, perTypeLimit);
        List<PluginCatalog> plugins = pluginCatalogRepository.searchByTitle(q, perTypeLimit);
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
