package com.boxai.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "box.oauth")
public class OAuthProperties {

    /** 对外可访问的后端根地址，用于拼接 OAuth 回调 URL（如 http://localhost:8080）。 */
    private String publicBaseUrl = "http://localhost:8080";
    private final Provider github = new Provider();
    private final Provider google = new Provider();

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public Provider getGithub() {
        return github;
    }

    public Provider getGoogle() {
        return google;
    }

    public static class Provider {
        private boolean enabled;
        private String clientId = "";
        private String clientSecret = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank()
                    && clientSecret != null && !clientSecret.isBlank();
        }
    }
}
