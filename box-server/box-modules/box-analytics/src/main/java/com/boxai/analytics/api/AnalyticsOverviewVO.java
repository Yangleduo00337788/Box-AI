package com.boxai.analytics.api;

import com.boxai.tenant.api.QuotaSnapshotVO;

import java.util.List;

public record AnalyticsOverviewVO(
        int agentCount,
        int conversationCount,
        int workspaceCount,
        int executionCount,
        int knowledgeBaseCount,
        int toolCount,
        int workflowCount,
        int mcpServerCount,
        QuotaSnapshotVO quota,
        int periodDays,
        int periodExecutionCount,
        double successRate,
        long avgLatencyMs,
        List<RecentAgentVO> recentAgents,
        List<RecentConversationVO> recentConversations,
        List<RecentWorkflowVO> recentWorkflows,
        List<TopAgentVO> topAgents
) {
}
