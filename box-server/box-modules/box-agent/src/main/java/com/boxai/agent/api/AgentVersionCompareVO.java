package com.boxai.agent.api;

import java.util.List;

public record AgentVersionCompareVO(
        Long baseVersionId,
        Long targetVersionId,
        List<AgentVersionDiffVO> diffs
) {}
