package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.user.support.OAuthClientConfigResolver;
import com.boxai.user.support.OAuthRemoteClient;
import com.boxai.user.support.OAuthStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthApplicationServiceTest {

    @Mock
    private OAuthClientConfigResolver clientConfigResolver;
    @Mock
    private OAuthStateStore stateStore;
    @Mock
    private OAuthRemoteClient remoteClient;
    @Mock
    private AuthApplicationService authApplicationService;

    private OAuthApplicationService service;

    @BeforeEach
    void setUp() {
        service = new OAuthApplicationService(
                clientConfigResolver, stateStore, remoteClient, authApplicationService);
    }

    @Test
    void listProvidersReturnsDisabledWhenNotConfigured() {
        when(clientConfigResolver.resolve("github")).thenReturn(disabled("github"));
        when(clientConfigResolver.resolve("google")).thenReturn(disabled("google"));
        when(clientConfigResolver.resolve("wechat")).thenReturn(disabled("wechat"));

        var providers = service.listProviders();
        assertEquals(3, providers.size());
        assertEquals("github", providers.get(0).provider());
        assertEquals("google", providers.get(1).provider());
        assertEquals("wechat", providers.get(2).provider());
        assertTrue(providers.stream().allMatch(item -> !item.enabled()));
    }

    @Test
    void buildAuthorizeUrlUsesGitHubWhenEnabled() {
        when(clientConfigResolver.callbackBaseUrl()).thenReturn("http://localhost:8080");
        when(clientConfigResolver.resolve("github"))
                .thenReturn(enabled("github", "gh-id", "gh-secret"));
        when(stateStore.create(any())).thenReturn("state-1");
        when(remoteClient.buildAuthorizeUrl(
                eq("github"),
                any(),
                eq("http://localhost:8080/api/v1/auth/oauth/github/callback"),
                eq("state-1")))
                .thenReturn("https://github.com/login/oauth/authorize?client_id=gh-id");

        String url = service.buildAuthorizeUrl(
                "github",
                "http://localhost:5173/login",
                "personal");
        assertTrue(url.contains("github.com"));
        verify(stateStore).create(any());
    }

    @Test
    void startAuthorizeRejectsUnknownProvider() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.buildAuthorizeUrl("feishu", "http://localhost:5173/login", "personal"));
        assertEquals(ErrorCode.OAUTH_PROVIDER_UNKNOWN, ex.getCode());
    }

    @Test
    void startAuthorizeReportsNotConfiguredForSupportedProvider() {
        when(clientConfigResolver.resolve("github")).thenReturn(disabled("github"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.buildAuthorizeUrl("GitHub", "http://localhost:5173/login", "personal"));
        assertEquals(ErrorCode.OAUTH_NOT_CONFIGURED, ex.getCode());
    }

    @Test
    void startAuthorizeRejectsOAuthOnEnterprisePortal() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.buildAuthorizeUrl("wechat", "http://localhost:5173/login", "enterprise"));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void startAuthorizeRejectsPersonalProviderOnEnterprisePortal() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.buildAuthorizeUrl("github", "http://localhost:5173/login", "enterprise"));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void buildErrorRedirectEncodesChineseMessage() {
        String redirect = service.buildErrorRedirect("missing-state", "该账号为企业账号，请切换到企业端登录");
        assertTrue(redirect.contains("oauth_error="));
        assertFalse(redirect.contains("该账号"));
    }

    @Test
    void handleCallbackBuildsRedirectWithToken() {
        when(clientConfigResolver.callbackBaseUrl()).thenReturn("http://localhost:8080");
        when(clientConfigResolver.resolve("google")).thenReturn(enabled("google", "g-id", "g-secret"));
        when(stateStore.consume("state-2")).thenReturn(java.util.Optional.of(
                new OAuthStateStore.OAuthStatePayload("google", "PERSONAL", "http://localhost:5173/chat")));
        when(remoteClient.exchangeAndFetchProfile(
                eq("google"),
                any(),
                eq("code-1"),
                eq("http://localhost:8080/api/v1/auth/oauth/google/callback")))
                .thenReturn(new OAuthRemoteClient.OAuthUserProfile(
                        "google", "sub-1", "user@example.com", "user@example.com", "User", null, null, null));
        when(authApplicationService.loginOrRegisterWithOAuth(any(), eq("PERSONAL"))).thenReturn("jwt-token");

        String redirect = service.handleCallbackAndBuildRedirect("google", "code-1", "state-2");
        assertTrue(redirect.contains("token=jwt-token"));
        assertTrue(redirect.startsWith("http://localhost:5173/chat"));
    }

    private static OAuthClientConfigResolver.ResolvedOAuthClient disabled(String provider) {
        return new OAuthClientConfigResolver.ResolvedOAuthClient(provider, false, null, null);
    }

    private static OAuthClientConfigResolver.ResolvedOAuthClient enabled(String provider, String id, String secret) {
        return new OAuthClientConfigResolver.ResolvedOAuthClient(provider, true, id, secret);
    }
}
