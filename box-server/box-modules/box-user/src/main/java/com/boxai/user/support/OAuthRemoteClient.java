package com.boxai.user.support;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OAuthRemoteClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public String buildAuthorizeUrl(String provider,
                                      OAuthClientConfigResolver.ResolvedOAuthClient client,
                                      String redirectUri,
                                      String state) {
        if ("github".equals(provider)) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("client_id", client.clientId());
            params.put("redirect_uri", redirectUri);
            params.put("scope", "read:user user:email");
            params.put("state", state);
            return "https://github.com/login/oauth/authorize?" + encode(params);
        }
        if ("google".equals(provider)) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("client_id", client.clientId());
            params.put("redirect_uri", redirectUri);
            params.put("response_type", "code");
            params.put("scope", "openid email profile");
            params.put("state", state);
            params.put("access_type", "online");
            params.put("prompt", "select_account");
            return "https://accounts.google.com/o/oauth2/v2/auth?" + encode(params);
        }
        if ("wechat".equals(provider)) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("appid", client.clientId());
            params.put("redirect_uri", redirectUri);
            params.put("response_type", "code");
            params.put("scope", "snsapi_login");
            params.put("state", state);
            return "https://open.weixin.qq.com/connect/qrconnect?" + encode(params) + "#wechat_redirect";
        }
        throw new BusinessException(ErrorCode.OAUTH_PROVIDER_UNKNOWN, "不支持的 OAuth 提供商: " + provider);
    }

    public OAuthUserProfile exchangeAndFetchProfile(String provider,
                                                    OAuthClientConfigResolver.ResolvedOAuthClient client,
                                                    String code,
                                                    String redirectUri) {
        try {
            if ("github".equals(provider)) {
                return fetchGitHubProfile(client, code, redirectUri);
            }
            if ("google".equals(provider)) {
                return fetchGoogleProfile(client, code, redirectUri);
            }
            if ("wechat".equals(provider)) {
                return fetchWeChatProfile(client, code);
            }
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_UNKNOWN, "不支持的 OAuth 提供商: " + provider);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "OAuth 登录失败: " + ex.getMessage());
        }
    }

    private OAuthUserProfile fetchGitHubProfile(OAuthClientConfigResolver.ResolvedOAuthClient client,
                                                String code,
                                                String redirectUri) throws Exception {
        Map<String, String> tokenFields = new LinkedHashMap<>();
        tokenFields.put("client_id", client.clientId());
        tokenFields.put("client_secret", client.clientSecret());
        tokenFields.put("code", code);
        tokenFields.put("redirect_uri", redirectUri);
        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/login/oauth/access_token"))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encode(tokenFields)))
                .build();
        JsonNode tokenJson = sendJson(tokenRequest);
        String accessToken = text(tokenJson, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "GitHub 未返回 access_token");
        }

        HttpRequest userRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/user"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        JsonNode userJson = sendJson(userRequest);
        String providerUserId = text(userJson, "id");
        String login = text(userJson, "login");
        String name = text(userJson, "name");
        String avatar = text(userJson, "avatar_url");
        String email = text(userJson, "email");
        if (email == null || email.isBlank()) {
            email = fetchPrimaryGitHubEmail(accessToken);
        }
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "GitHub 用户信息不完整");
        }
        return new OAuthUserProfile("github", providerUserId, email, login, name, avatar, null, null);
    }

    private String fetchPrimaryGitHubEmail(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/user/emails"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        JsonNode emails = sendJson(request);
        if (!emails.isArray()) {
            return null;
        }
        for (JsonNode item : emails) {
            if (item.path("primary").asBoolean(false) && item.path("verified").asBoolean(false)) {
                return text(item, "email");
            }
        }
        for (JsonNode item : emails) {
            if (item.path("verified").asBoolean(false)) {
                return text(item, "email");
            }
        }
        return null;
    }

    private OAuthUserProfile fetchGoogleProfile(OAuthClientConfigResolver.ResolvedOAuthClient client,
                                                String code,
                                                String redirectUri) throws Exception {
        Map<String, String> tokenFields = new LinkedHashMap<>();
        tokenFields.put("client_id", client.clientId());
        tokenFields.put("client_secret", client.clientSecret());
        tokenFields.put("code", code);
        tokenFields.put("redirect_uri", redirectUri);
        tokenFields.put("grant_type", "authorization_code");
        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encode(tokenFields)))
                .build();
        JsonNode tokenJson = sendJson(tokenRequest);
        String accessToken = text(tokenJson, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "Google 未返回 access_token");
        }

        HttpRequest userRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://openidconnect.googleapis.com/v1/userinfo"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        JsonNode userJson = sendJson(userRequest);
        String providerUserId = text(userJson, "sub");
        String email = text(userJson, "email");
        String name = text(userJson, "name");
        String avatar = text(userJson, "picture");
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "Google 用户信息不完整");
        }
        return new OAuthUserProfile("google", providerUserId, email, email, name, avatar, null, null);
    }

    private OAuthUserProfile fetchWeChatProfile(OAuthClientConfigResolver.ResolvedOAuthClient client,
                                               String code) throws Exception {
        String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?"
                + encode(Map.of(
                "appid", client.clientId(),
                "secret", client.clientSecret(),
                "code", code,
                "grant_type", "authorization_code"));
        JsonNode tokenJson = sendJson(HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build());
        requireWeChatOk(tokenJson, "微信换票失败");
        String accessToken = text(tokenJson, "access_token");
        String openId = text(tokenJson, "openid");
        if (accessToken == null || openId == null) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "微信未返回 access_token");
        }
        String userUrl = "https://api.weixin.qq.com/sns/userinfo?"
                + encode(Map.of(
                "access_token", accessToken,
                "openid", openId,
                "lang", "zh_CN"));
        JsonNode userJson = sendJson(HttpRequest.newBuilder()
                .uri(URI.create(userUrl))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build());
        requireWeChatOk(userJson, "微信获取用户失败");
        String providerUserId = firstNonBlank(text(userJson, "unionid"), text(userJson, "openid"), openId);
        String name = text(userJson, "nickname");
        String avatar = text(userJson, "headimgurl");
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "微信用户信息不完整");
        }
        return new OAuthUserProfile("wechat", providerUserId, null, name, name, avatar, null, null);
    }

    private JsonNode sendJson(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED,
                    "OAuth 远程请求失败 (" + response.statusCode() + ")");
        }
        return MAPPER.readTree(response.body());
    }

    private static void requireWeChatOk(JsonNode node, String message) {
        if (node.has("errcode") && node.path("errcode").asInt(0) != 0) {
            String detail = text(node, "errmsg");
            throw new BusinessException(ErrorCode.OAUTH_FAILED,
                    message + (detail == null ? "" : ": " + detail));
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private static String encode(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> url(entry.getKey()) + "=" + url(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record OAuthUserProfile(
            String provider,
            String providerUserId,
            String email,
            String usernameHint,
            String displayName,
            String avatarUrl,
            String organizationId,
            String organizationName
    ) {
        public String normalizedEmail() {
            if (email == null || email.isBlank()) {
                return null;
            }
            return email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
