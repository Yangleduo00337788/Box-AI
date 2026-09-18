package com.boxai.agent.api;

import java.util.List;

public record AgentEmbedConfigVO(
        String themeColor,
        String logoUrl,
        String welcomeMessage,
        List<String> suggestedQuestions,
        String agentName,
        String customDomain,
        Boolean domainVerified,
        String domainVerifyToken,
        Boolean domainVerifySkipped,
        String gatewayCnameTarget,
        String gatewayTlsMode,
        String gatewaySetupHint
) {
}
