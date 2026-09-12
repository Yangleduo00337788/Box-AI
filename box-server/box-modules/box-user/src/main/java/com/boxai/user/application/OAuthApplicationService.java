package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.user.api.OAuthProviderVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OAuthApplicationService {

    private static final Map<String, String> PROVIDER_NAMES = Map.of(
            "github", "GitHub",
            "google", "Google",
            "sso", "企业 SSO"
    );

    private static final Set<String> SUPPORTED_PROVIDERS = PROVIDER_NAMES.keySet();

    public List<OAuthProviderVO> listProviders() {
        return SUPPORTED_PROVIDERS.stream()
                .sorted()
                .map(this::toProvider)
                .toList();
    }

    public void startAuthorize(String provider, String redirectUri) {
        resolveProvider(provider);
        throw new BusinessException(
                ErrorCode.OAUTH_NOT_CONFIGURED,
                "OAuth 登录尚未配置，请在 box.oauth 中启用 " + provider + " 并完成客户端注册");
    }

    public void handleCallback(String provider, String code, String state) {
        resolveProvider(provider);
        throw new BusinessException(ErrorCode.OAUTH_NOT_CONFIGURED, "OAuth 回调尚未实现");
    }

    private OAuthProviderVO toProvider(String provider) {
        return new OAuthProviderVO(
                provider,
                PROVIDER_NAMES.get(provider),
                false,
                "/api/v1/auth/oauth/" + provider + "/authorize");
    }

    private String resolveProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_PROVIDERS.contains(normalized)) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_UNKNOWN, "不支持的 OAuth 提供商: " + provider);
        }
        return normalized;
    }
}
