package com.boxai.tenant.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "box.payment")
public class PaymentProperties {

    private boolean enabled = false;
    private String provider = "alipay";
    private String successUrl = "http://localhost:5173/settings/plan?tab=billing&payment=success";
    private String cancelUrl = "http://localhost:5173/settings/plan?tab=plans&payment=cancel";
    /** 未支付订单自动关闭分钟数，同时写入支付宝 timeout_express。 */
    private int pendingExpireMinutes = 10;
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

    public int getPendingExpireMinutes() {
        return pendingExpireMinutes < 1 ? 10 : pendingExpireMinutes;
    }

    public void setPendingExpireMinutes(int pendingExpireMinutes) {
        this.pendingExpireMinutes = pendingExpireMinutes;
    }

    public Alipay getAlipay() {
        return alipay;
    }

    public String resolvedProvider() {
        return provider == null || provider.isBlank() ? "alipay" : provider.trim().toLowerCase();
    }

    public boolean isRealGatewayConfigured() {
        return "alipay".equals(resolvedProvider())
                && alipay.getAppId() != null && !alipay.getAppId().isBlank()
                && alipay.getPrivateKey() != null && !alipay.getPrivateKey().isBlank();
    }

    public static class Alipay {
        private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
        private String appId = "";
        private String privateKey = "";
        private String alipayPublicKey = "";
        private String notifyUrl = "";
        /** 浏览器支付完成回跳，先打到后端查单再 302 到前端。 */
        private String returnUrl = "";

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

        public String getReturnUrl() {
            return returnUrl;
        }

        public void setReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
        }
    }
}
