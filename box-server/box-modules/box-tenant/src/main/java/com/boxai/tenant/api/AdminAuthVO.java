package com.boxai.tenant.api;

public record AdminAuthVO(
        String token,
        AdminUserVO user
) {}
