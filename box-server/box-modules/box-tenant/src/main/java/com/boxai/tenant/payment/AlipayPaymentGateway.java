package com.boxai.tenant.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class AlipayPaymentGateway implements PaymentGateway {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter ALIPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String channel() {
        return "ALIPAY";
    }

    @Override
    public boolean supports(PaymentProperties properties) {
        return "alipay".equalsIgnoreCase(properties.getProvider())
                && properties.getAlipay().getAppId() != null
                && !properties.getAlipay().getAppId().isBlank()
                && properties.getAlipay().getPrivateKey() != null
                && !properties.getAlipay().getPrivateKey().isBlank();
    }

    @Override
    public PaymentCheckoutResult createCheckout(PaymentCheckoutCommand command, PaymentProperties properties) {
        try {
            String outTradeNo = "BOX-PAY-" + command.paymentId();
            Map<String, String> biz = new LinkedHashMap<>();
            biz.put("out_trade_no", outTradeNo);
            biz.put("product_code", "FAST_INSTANT_TRADE_PAY");
            biz.put("total_amount", command.amount().setScale(2, RoundingMode.HALF_UP).toPlainString());
            biz.put("subject", command.subject());
            biz.put("body", command.invoiceNo());

            Map<String, String> params = baseParams(properties);
            params.put("method", "alipay.trade.page.pay");
            params.put("return_url", properties.getSuccessUrl());
            params.put("notify_url", resolveNotifyUrl(properties));
            params.put("biz_content", MAPPER.writeValueAsString(biz));
            params.put("sign", sign(params, properties.getAlipay().getPrivateKey()));

            String query = params.entrySet().stream()
                    .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                    .collect(Collectors.joining("&"));
            String paymentUrl = properties.getAlipay().getGatewayUrl() + "?" + query;
            return new PaymentCheckoutResult(channel(), paymentUrl, outTradeNo, false);
        } catch (Exception e) {
            throw new IllegalStateException("支付宝支付初始化失败", e);
        }
    }

    @Override
    public Optional<Long> resolvePaymentIdFromWebhook(String rawBody, Map<String, String> headers, PaymentProperties properties) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> resolvePaymentIdFromNotify(Map<String, String> params, PaymentProperties properties) {
        if (params == null || params.isEmpty()) {
            return Optional.empty();
        }
        if (!verifyNotify(params, properties.getAlipay().getAlipayPublicKey())) {
            return Optional.empty();
        }
        if (!"TRADE_SUCCESS".equalsIgnoreCase(params.get("trade_status"))
                && !"TRADE_FINISHED".equalsIgnoreCase(params.get("trade_status"))) {
            return Optional.empty();
        }
        String outTradeNo = params.get("out_trade_no");
        if (outTradeNo == null || !outTradeNo.startsWith("BOX-PAY-")) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(outTradeNo.substring("BOX-PAY-".length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Map<String, String> baseParams(PaymentProperties properties) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", properties.getAlipay().getAppId());
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", ALIPAY_TIME.format(LocalDateTime.now()));
        params.put("version", "1.0");
        return params;
    }

    private static String resolveNotifyUrl(PaymentProperties properties) {
        if (properties.getAlipay().getNotifyUrl() != null && !properties.getAlipay().getNotifyUrl().isBlank()) {
            return properties.getAlipay().getNotifyUrl();
        }
        return "http://127.0.0.1:8080/api/v1/billing/payments/notify/alipay";
    }

    private static String sign(Map<String, String> params, String privateKeyPem) throws Exception {
        String content = params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .filter(entry -> !"sign".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(readPrivateKey(privateKeyPem));
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private static boolean verifyNotify(Map<String, String> params, String publicKeyPem) {
        try {
            String sign = params.get("sign");
            if (sign == null || sign.isBlank()) {
                return false;
            }
            String content = new TreeMap<>(params).entrySet().stream()
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                    .filter(entry -> !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey()))
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(readPublicKey(publicKeyPem));
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }

    private static PrivateKey readPrivateKey(String pem) throws Exception {
        String normalized = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private static PublicKey readPublicKey(String pem) throws Exception {
        String normalized = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
