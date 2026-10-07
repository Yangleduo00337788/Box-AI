package com.boxai.tenant.api;

public record EnterpriseOrgAccessVO(
        Long tenantId,
        String name,
        String orgId,
        String inviteCode
) {}
