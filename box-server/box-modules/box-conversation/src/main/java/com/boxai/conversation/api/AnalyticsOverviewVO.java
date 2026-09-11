package com.boxai.conversation.api;

import com.boxai.tenant.api.QuotaSnapshotVO;

public record AnalyticsOverviewVO(
        int agentCount,
        int conversationCount,
        int workspaceCount,
        int executionCount,
        int knowledgeBaseCount,
        int toolCount,
        int workflowCount,
        int mcpServerCount,
        QuotaSnapshotVO quota
) {}
