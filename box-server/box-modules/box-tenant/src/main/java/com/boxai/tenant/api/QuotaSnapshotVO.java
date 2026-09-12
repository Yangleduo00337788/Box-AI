package com.boxai.tenant.api;

public record QuotaSnapshotVO(
        Long tenantId,
        Long planId,
        String planName,
        String period,
        Integer quotaAiCalls,
        Long quotaTokens,
        Integer quotaMembers,
        Integer quotaWorkspaces,
        Integer quotaKnowledgeBases,
        Integer usedAiCalls,
        Long usedTokens,
        Integer usedMembers,
        Integer usedWorkspaces,
        Integer usedKnowledgeBases,
        Integer remainingAiCalls,
        Long remainingTokens,
        Integer remainingMembers,
        Integer remainingWorkspaces,
        Integer remainingKnowledgeBases
) {}
