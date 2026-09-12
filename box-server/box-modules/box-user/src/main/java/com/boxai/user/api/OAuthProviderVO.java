package com.boxai.user.api;

public record OAuthProviderVO(
        String provider,
        String displayName,
        boolean enabled,
        String authorizePath
) {
}
