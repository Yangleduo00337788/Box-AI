package com.boxai.conversation.application;

import com.boxai.conversation.api.AnalyticsOverviewVO;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.conversation.ConversationRepository;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.domain.trace.ExecutionRepository;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.tenant.application.QuotaApplicationService;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsApplicationService {

    private final AgentRepository agentRepository;
    private final ConversationRepository conversationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ExecutionRepository executionRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ToolRepository toolRepository;
    private final WorkflowRepository workflowRepository;
    private final McpServerRepository mcpServerRepository;
    private final QuotaApplicationService quotaApplicationService;

    public AnalyticsApplicationService(AgentRepository agentRepository,
                                       ConversationRepository conversationRepository,
                                       WorkspaceRepository workspaceRepository,
                                       ExecutionRepository executionRepository,
                                       KnowledgeBaseRepository knowledgeBaseRepository,
                                       ToolRepository toolRepository,
                                       WorkflowRepository workflowRepository,
                                       McpServerRepository mcpServerRepository,
                                       QuotaApplicationService quotaApplicationService) {
        this.agentRepository = agentRepository;
        this.conversationRepository = conversationRepository;
        this.workspaceRepository = workspaceRepository;
        this.executionRepository = executionRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.toolRepository = toolRepository;
        this.workflowRepository = workflowRepository;
        this.mcpServerRepository = mcpServerRepository;
        this.quotaApplicationService = quotaApplicationService;
    }

    public AnalyticsOverviewVO overview() {
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        return new AnalyticsOverviewVO(
                agentRepository.countByWorkspace(workspaceId),
                conversationRepository.countByWorkspaceAndUser(workspaceId, userId),
                workspaceRepository.listMembersByUserId(userId).size(),
                executionRepository.countByWorkspace(workspaceId),
                knowledgeBaseRepository.listByWorkspace(workspaceId).size(),
                toolRepository.listByWorkspace(workspaceId).size(),
                workflowRepository.listByWorkspace(workspaceId).size(),
                mcpServerRepository.listByWorkspace(workspaceId).size(),
                quotaApplicationService.getQuotaForUser(userId));
    }
}
