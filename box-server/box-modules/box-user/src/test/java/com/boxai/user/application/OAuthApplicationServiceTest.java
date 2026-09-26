package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthApplicationServiceTest {

    private final OAuthApplicationService service = new OAuthApplicationService();

    @Test
    void listProvidersReturnsDisabledPlaceholders() {
        var providers = service.listProviders();
        assertEquals(3, providers.size());
        assertTrue(providers.stream().anyMatch(item -> "github".equals(item.provider())));
        assertTrue(providers.stream().allMatch(item -> !item.enabled()));
    }

    @Test
    void startAuthorizeRejectsUnknownProvider() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.startAuthorize("facebook", "https://app.example/callback"));
        assertEquals(ErrorCode.OAUTH_PROVIDER_UNKNOWN, ex.getCode());
    }

    @Test
    void startAuthorizeReportsNotConfiguredForSupportedProvider() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.startAuthorize("GitHub", "https://app.example/callback"));
        assertEquals(ErrorCode.OAUTH_NOT_CONFIGURED, ex.getCode());
    }

    @Test
    void startAuthorizeReportsNotConfiguredForSso() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.startAuthorize("SSO", "https://app.example/callback"));
        assertEquals(ErrorCode.OAUTH_NOT_CONFIGURED, ex.getCode());
        assertTrue(ex.getMessage().toLowerCase().contains("sso"));
    }

    @Test
    void handleCallbackIsNotImplemented() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.handleCallback("google", "code", "state"));
        assertEquals(ErrorCode.OAUTH_NOT_CONFIGURED, ex.getCode());
        assertFalse(ex.getMessage().isBlank());
    }
}
