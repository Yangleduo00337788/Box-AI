package com.boxai.tenant.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "box.payment")
public class PaymentProperties {

    private boolean enabled = false;
    private String provider = "mock";
    private String successUrl = "http://127.0.0.1:5173/settings/plan?tab=billing&payment=success";
    private String cancelUrl = "http://127.0.0.1:5173/settings/plan?tab=plans&payment=cancel";
    private final Stripe stripe = new Stripe();
    private final Alipay alipay = new Alipay();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public Stripe getStripe() {
        return stripe;
    }

    public Alipay getAlipay() {
        return alipay;
    }

    public boolean isRealGatewayConfigured() {
        String normalized = provider == null ? "mock" : provider.trim().toLowerCase();
        return switch (normalized) {
            case "stripe" -> stripe.getSecretKey() != null && !stripe.getSecretKey().isBlank();
            case "alipay" -> alipay.getAppId() != null && !alipay.getAppId().isBlank()
                    && alipay.getPrivateKey() != null && !alipay.getPrivateKey().isBlank();
            default -> false;
        };
    }

    public static class Stripe {
        private String secretKey = "";
        private String webhookSecret = "";

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }

    public static class Alipay {
        private String gatewayUrl = "https://openapi.alipay.com/gateway.do";
        private String appId = "";
        private String privateKey = "";
        private String alipayPublicKey = "";
        private String notifyUrl = "";

        public String getGatewayUrl() {
            return gatewayUrl;
        }

        public void setGatewayUrl(String gatewayUrl) {
            this.gatewayUrl = gatewayUrl;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public String getAlipayPublicKey() {
            return alipayPublicKey;
        }

        public void setAlipayPublicKey(String alipayPublicKey) {
            this.alipayPublicKey = alipayPublicKey;
        }

        public String getNotifyUrl() {
            return notifyUrl;
        }

        public void setNotifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
        }
    }
}
