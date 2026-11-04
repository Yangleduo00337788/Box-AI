package com.boxai.user.application;

import com.boxai.common.constant.TenantTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.user.api.OAuthProviderVO;
import com.boxai.user.config.OAuthProperties;
import com.boxai.user.support.OAuthClientConfigResolver;
import com.boxai.user.support.OAuthRemoteClient;
import com.boxai.user.support.OAuthStateStore;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OAuthApplicationService {

    private static final Map<String, String> PROVIDER_NAMES = Map.of(
            "github", "GitHub",
            "google", "Google"
    );

    private static final List<String> PROVIDER_ORDER = List.of("github", "google");
    private static final Set<String> SUPPORTED_PROVIDERS = Set.copyOf(PROVIDER_ORDER);

    private final OAuthClientConfigResolver clientConfigResolver;
    private final OAuthStateStore stateStore;
    private final OAuthRemoteClient remoteClient;
    private final AuthApplicationService authApplicationService;
    private final OAuthProperties oauthProperties;

    public OAuthApplicationService(OAuthClientConfigResolver clientConfigResolver,
                                   OAuthStateStore stateStore,
                                   OAuthRemoteClient remoteClient,
                                   AuthApplicationService authApplicationService,
                                   OAuthProperties oauthProperties) {
        this.clientConfigResolver = clientConfigResolver;
        this.stateStore = stateStore;
        this.remoteClient = remoteClient;
        this.authApplicationService = authApplicationService;
        this.oauthProperties = oauthProperties;
    }

    public List<OAuthProviderVO> listProviders() {
        return PROVIDER_ORDER.stream()
                .map(this::toProvider)
                .toList();
    }

    public String buildAuthorizeUrl(String provider, String redirectUri, String portal) {
        String normalized = resolveProvider(provider);
        String portalType = normalizePortal(portal);
        assertProviderMatchesPortal(normalized, portalType);
        OAuthClientConfigResolver.ResolvedOAuthClient client = requireEnabledClient(normalized);
        String safeRedirect = sanitizeFrontendRedirect(redirectUri, oauthProperties);
        String state = stateStore.create(new OAuthStateStore.OAuthStatePayload(
                normalized,
                portalType,
                safeRedirect));
        String callbackUri = callbackUri(normalized);
        return remoteClient.buildAuthorizeUrl(normalized, client, callbackUri, state);
    }

    public String handleCallbackAndBuildRedirect(String provider, String code, String state) {
        String normalized = resolveProvider(provider);
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "缺少授权码");
        }
        OAuthStateStore.OAuthStatePayload payload = stateStore.consume(state)
                .orElseThrow(() -> new BusinessException(ErrorCode.OAUTH_FAILED, "登录状态已失效，请重试"));
        if (!normalized.equals(payload.provider())) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "OAuth 状态与提供商不匹配");
        }
        assertProviderMatchesPortal(normalized, payload.portal());
        OAuthClientConfigResolver.ResolvedOAuthClient client = requireEnabledClient(normalized);
        OAuthRemoteClient.OAuthUserProfile profile = remoteClient.exchangeAndFetchProfile(
                normalized, client, code.trim(), callbackUri(normalized));
        String token = authApplicationService.loginOrRegisterWithOAuth(profile, payload.portal());
        return appendToken(payload.redirectUri(), token);
    }

    public String buildErrorRedirect(String state, String message) {
        stateStore.consume(state);
        String error = message == null || message.isBlank() ? "登录失败" : message.trim();
        return UriComponentsBuilder.fromUriString(defaultFrontendRedirect(oauthProperties))
                .queryParam("oauth_error", error)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    private OAuthClientConfigResolver.ResolvedOAuthClient requireEnabledClient(String provider) {
        OAuthClientConfigResolver.ResolvedOAuthClient client = clientConfigResolver.resolve(provider);
        if (!client.enabled()) {
            throw new BusinessException(
                    ErrorCode.OAUTH_NOT_CONFIGURED,
                    "OAuth 登录尚未配置，请在管理端或 box.oauth 中启用 " + provider + " 并填写 Client ID/Secret");
        }
        return client;
    }

    private static void assertProviderMatchesPortal(String provider, String portal) {
        if (TenantTypes.ENTERPRISE.equals(portal)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "企业请使用企业邮箱或企业标识登录");
        }
    }

    private OAuthProviderVO toProvider(String provider) {
        boolean enabled = clientConfigResolver.resolve(provider).enabled();
        return new OAuthProviderVO(
                provider,
                PROVIDER_NAMES.get(provider),
                enabled,
                "/api/v1/auth/oauth/" + provider + "/authorize");
    }

    private String callbackUri(String provider) {
        return clientConfigResolver.callbackBaseUrl()
                + "/api/v1/auth/oauth/" + provider + "/callback";
    }

    private String resolveProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_PROVIDERS.contains(normalized)) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_UNKNOWN, "不支持的 OAuth 提供商: " + provider);
        }
        return normalized;
    }

    private static String normalizePortal(String portal) {
        if (portal == null || portal.isBlank()) {
            return TenantTypes.PERSONAL;
        }
        if (TenantTypes.ENTERPRISE.equalsIgnoreCase(portal)) {
            return TenantTypes.ENTERPRISE;
        }
        return TenantTypes.PERSONAL;
    }

    static String sanitizeFrontendRedirect(String redirectUri, OAuthProperties properties) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return defaultFrontendRedirect(properties);
        }
        String trimmed = redirectUri.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "redirectUri 必须是完整 URL");
        }
        try {
            URI uri = URI.create(trimmed);
            if (uri.getHost() == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "redirectUri 无效");
            }
            String origin = originOf(uri);
            if (!allowedOrigins(properties).contains(origin)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "redirectUri 不在允许的前端域名列表中");
            }
            return trimmed;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "redirectUri 无效");
        }
    }

    private static String defaultFrontendRedirect(OAuthProperties properties) {
        java.util.List<String> origins = allowedOrigins(properties);
        String origin = origins.isEmpty() ? "http://localhost:5173" : origins.get(0);
        return origin + "/login";
    }

    private static java.util.List<String> allowedOrigins(OAuthProperties properties) {
        String raw = properties == null ? null : properties.getAllowedRedirectOrigins();
        if (raw == null || raw.isBlank()) {
            return java.util.List.of("http://localhost:5173", "http://127.0.0.1:5173");
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private static String originOf(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        boolean defaultPort = port == -1
                || ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return defaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
    }

    private static String appendToken(String redirectUri, String token) {
        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }
}
