package com.boxai.user.api;

import java.util.List;

public record AuthVO(
        String token,
        UserVO user,
        TenantSummaryVO tenant,
        List<WorkspaceVO> workspaces
) {}
