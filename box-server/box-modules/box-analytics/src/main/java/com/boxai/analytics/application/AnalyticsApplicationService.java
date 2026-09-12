package com.boxai.analytics.application;

import com.boxai.analytics.api.AnalyticsOverviewVO;
import com.boxai.analytics.api.AnalyticsTrendPointVO;
import com.boxai.analytics.api.AnalyticsTrendsVO;
import com.boxai.analytics.api.RecentAgentVO;
import com.boxai.analytics.api.RecentConversationVO;
import com.boxai.analytics.api.RecentWorkflowVO;
import com.boxai.analytics.api.TopAgentVO;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.conversation.Conversation;
import com.boxai.domain.conversation.ConversationRepository;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.domain.trace.Execution;
import com.boxai.domain.trace.ExecutionRepository;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tenant.application.QuotaApplicationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final WorkspacePermissionService workspacePermissionService;

    public AnalyticsApplicationService(AgentRepository agentRepository,
                                       ConversationRepository conversationRepository,
                                       WorkspaceRepository workspaceRepository,
                                       ExecutionRepository executionRepository,
                                       KnowledgeBaseRepository knowledgeBaseRepository,
                                       ToolRepository toolRepository,
                                       WorkflowRepository workflowRepository,
                                       McpServerRepository mcpServerRepository,
                                       QuotaApplicationService quotaApplicationService,
                                       WorkspacePermissionService workspacePermissionService) {
        this.agentRepository = agentRepository;
        this.conversationRepository = conversationRepository;
        this.workspaceRepository = workspaceRepository;
        this.executionRepository = executionRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.toolRepository = toolRepository;
        this.workflowRepository = workflowRepository;
        this.mcpServerRepository = mcpServerRepository;
        this.quotaApplicationService = quotaApplicationService;
        this.workspacePermissionService = workspacePermissionService;
    }

    public AnalyticsOverviewVO overview(int days) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        int periodDays = days <= 0 ? 7 : Math.min(days, 90);
        LocalDateTime since = LocalDateTime.now().minusDays(periodDays);
        List<Execution> periodExecutions = executionRepository.listByWorkspace(workspaceId, 500).stream()
                .filter(item -> item.getStartedAt() != null && !item.getStartedAt().isBefore(since))
                .toList();
        int succeeded = periodExecutions.stream()
                .filter(item -> "SUCCEEDED".equalsIgnoreCase(item.getStatus()) || "SUCCESS".equalsIgnoreCase(item.getStatus()))
                .toList()
                .size();
        double successRate = periodExecutions.isEmpty() ? 0D : succeeded * 100D / periodExecutions.size();
        long avgLatencyMs = (long) periodExecutions.stream()
                .map(Execution::getDurationMs)
                .filter(value -> value != null && value > 0)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0D);

        List<RecentAgentVO> recentAgents = agentRepository.listByWorkspace(workspaceId).stream()
                .sorted(Comparator.comparing(Agent::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(item -> new RecentAgentVO(item.getId(), item.getName(), item.getStatus(), item.getUpdatedAt()))
                .toList();
        List<RecentConversationVO> recentConversations = conversationRepository
                .listByWorkspaceAndUser(workspaceId, userId).stream()
                .sorted(Comparator.comparing(
                        (Conversation item) -> item.getLastMessageAt() == null ? item.getUpdatedAt() : item.getLastMessageAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(item -> new RecentConversationVO(
                        item.getId(),
                        item.getAgentId(),
                        item.getTitle(),
                        item.getLastMessageAt(),
                        item.getUpdatedAt()))
                .toList();
        List<RecentWorkflowVO> recentWorkflows = workflowRepository.listByWorkspace(workspaceId).stream()
                .sorted(Comparator.comparing(Workflow::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(item -> new RecentWorkflowVO(item.getId(), item.getName(), item.getStatus(), item.getUpdatedAt()))
                .toList();
        Map<Long, String> agentNames = agentRepository.listByWorkspace(workspaceId).stream()
                .collect(Collectors.toMap(Agent::getId, Agent::getName, (left, right) -> left));
        List<TopAgentVO> topAgents = periodExecutions.stream()
                .filter(item -> item.getAgentId() != null)
                .collect(Collectors.groupingBy(Execution::getAgentId))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<Execution> items = entry.getValue();
                    int total = items.size();
                    int ok = items.stream()
                            .filter(item -> "SUCCEEDED".equalsIgnoreCase(item.getStatus())
                                    || "SUCCESS".equalsIgnoreCase(item.getStatus()))
                            .toList()
                            .size();
                    return new TopAgentVO(
                            entry.getKey(),
                            agentNames.getOrDefault(entry.getKey(), "Agent #" + entry.getKey()),
                            total,
                            total == 0 ? 0D : ok * 100D / total);
                })
                .sorted(Comparator.comparingInt(TopAgentVO::executionCount).reversed())
                .limit(5)
                .toList();

        return new AnalyticsOverviewVO(
                agentRepository.countByWorkspace(workspaceId),
                conversationRepository.countByWorkspaceAndUser(workspaceId, userId),
                workspaceRepository.listMembersByUserId(userId).size(),
                executionRepository.countByWorkspace(workspaceId),
                knowledgeBaseRepository.listByWorkspace(workspaceId).size(),
                toolRepository.listByWorkspace(workspaceId).size(),
                workflowRepository.listByWorkspace(workspaceId).size(),
                mcpServerRepository.listByWorkspace(workspaceId).size(),
                quotaApplicationService.getQuotaForUser(userId),
                periodDays,
                periodExecutions.size(),
                successRate,
                avgLatencyMs,
                recentAgents,
                recentConversations,
                recentWorkflows,
                topAgents);
    }

    public AnalyticsTrendsVO trends(int days) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        int periodDays = days <= 0 ? 7 : Math.min(days, 90);
        LocalDate today = LocalDate.now();
        LocalDateTime since = today.minusDays(periodDays - 1).atStartOfDay();
        List<Execution> periodExecutions = executionRepository.listByWorkspace(workspaceId, 1000).stream()
                .filter(item -> item.getStartedAt() != null && !item.getStartedAt().isBefore(since))
                .toList();

        Map<LocalDate, List<Execution>> grouped = new LinkedHashMap<>();
        for (int offset = periodDays - 1; offset >= 0; offset--) {
            grouped.put(today.minusDays(offset), new ArrayList<>());
        }
        for (Execution execution : periodExecutions) {
            LocalDate date = execution.getStartedAt().toLocalDate();
            grouped.computeIfAbsent(date, key -> new ArrayList<>()).add(execution);
        }

        List<AnalyticsTrendPointVO> points = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<Execution> items = entry.getValue();
                    int total = items.size();
                    int succeeded = items.stream()
                            .filter(item -> "SUCCEEDED".equalsIgnoreCase(item.getStatus())
                                    || "SUCCESS".equalsIgnoreCase(item.getStatus()))
                            .toList()
                            .size();
                    double successRate = total == 0 ? 0D : succeeded * 100D / total;
                    long avgLatencyMs = (long) items.stream()
                            .map(Execution::getDurationMs)
                            .filter(value -> value != null && value > 0)
                            .mapToLong(Long::longValue)
                            .average()
                            .orElse(0D);
                    return new AnalyticsTrendPointVO(entry.getKey().toString(), total, successRate, avgLatencyMs);
                })
                .toList();
        return new AnalyticsTrendsVO(periodDays, points);
    }
}
