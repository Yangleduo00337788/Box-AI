package com.boxai.tenant.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class AlipayPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(AlipayPaymentGateway.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter ALIPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String OUT_TRADE_PREFIX = "BOX-PAY-";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public String channel() {
        return "ALIPAY";
    }

    @Override
    public boolean supports(PaymentProperties properties) {
        return "alipay".equalsIgnoreCase(properties.resolvedProvider())
                && properties.getAlipay().getAppId() != null
                && !properties.getAlipay().getAppId().isBlank()
                && properties.getAlipay().getPrivateKey() != null
                && !properties.getAlipay().getPrivateKey().isBlank();
    }

    @Override
    public PaymentCheckoutResult createCheckout(PaymentCheckoutCommand command, PaymentProperties properties) {
        try {
            String outTradeNo = OUT_TRADE_PREFIX + command.paymentId();
            Map<String, String> biz = new LinkedHashMap<>();
            biz.put("out_trade_no", outTradeNo);
            biz.put("product_code", "FAST_INSTANT_TRADE_PAY");
            biz.put("total_amount", command.amount().setScale(2, RoundingMode.HALF_UP).toPlainString());
            biz.put("subject", command.subject());
            biz.put("body", command.invoiceNo());
            biz.put("passback_params", String.valueOf(command.paymentId()));
            int expireMinutes = properties.getPendingExpireMinutes();
            biz.put("timeout_express", expireMinutes + "m");
            biz.put("time_expire", ALIPAY_TIME.format(LocalDateTime.now().plusMinutes(expireMinutes)));

            Map<String, String> params = baseParams(properties);
            params.put("method", "alipay.trade.page.pay");
            params.put("return_url", PaymentUrls.withQuery(resolveReturnUrl(properties), "paymentId", String.valueOf(command.paymentId())));
            params.put("notify_url", resolveNotifyUrl(properties));
            params.put("biz_content", MAPPER.writeValueAsString(biz));
            params.put("sign", sign(params, properties.getAlipay().getPrivateKey()));

            String gateway = properties.getAlipay().getGatewayUrl();
            String formAction = gateway + "?charset=" + params.get("charset");
            return new PaymentCheckoutResult(
                    channel(),
                    null,
                    outTradeNo,
                    false,
                    formAction,
                    Map.copyOf(params));
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
            log.warn("支付宝异步通知验签失败");
            return Optional.empty();
        }
        if (!isPaidTradeStatus(params.get("trade_status"))) {
            return Optional.empty();
        }
        return parsePaymentId(params.get("out_trade_no"), params.get("passback_params"));
    }

    public boolean isVerifiedNonPaidNotify(Map<String, String> params, PaymentProperties properties) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        if (!verifyNotify(params, properties.getAlipay().getAlipayPublicKey())) {
            return false;
        }
        return !isPaidTradeStatus(params.get("trade_status"));
    }

    @Override
    public Optional<String> queryPaidExternalRef(String externalRef, Long paymentId, PaymentProperties properties) {
        String outTradeNo = paymentId != null ? OUT_TRADE_PREFIX + paymentId : externalRef;
        if (outTradeNo == null || outTradeNo.isBlank()) {
            return Optional.empty();
        }
        try {
            Map<String, String> biz = Map.of("out_trade_no", outTradeNo);
            Map<String, String> params = baseParams(properties);
            params.put("method", "alipay.trade.query");
            params.put("biz_content", MAPPER.writeValueAsString(biz));
            params.put("sign", sign(params, properties.getAlipay().getPrivateKey()));
            String body = params.entrySet().stream()
                    .map(entry -> PaymentUrls.encode(entry.getKey()) + "=" + PaymentUrls.encode(entry.getValue()))
                    .collect(Collectors.joining("&"));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getAlipay().getGatewayUrl() + "?charset=UTF-8"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.debug("支付宝查单失败: HTTP {}", response.statusCode());
                return Optional.empty();
            }
            Optional<String> paid = parsePaidTradeNo(response.body());
            if (paid.isEmpty()) {
                if (isUncreatedTrade(response.body())) {
                    log.debug("支付宝查单交易不存在 out_trade_no={}", outTradeNo);
                } else {
                    log.warn("支付宝查单未确认支付 out_trade_no={} body={}", outTradeNo, truncate(response.body()));
                }
            }
            return paid;
        } catch (Exception e) {
            log.warn("支付宝查单异常: {}", e.getMessage());
            return Optional.empty();
        }
    }

    static Optional<String> parsePaidTradeNo(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = MAPPER.readTree(rawBody);
            JsonNode payload = root.path("alipay_trade_query_response");
            if (!"10000".equals(payload.path("code").asText())) {
                return Optional.empty();
            }
            if (!isPaidTradeStatus(payload.path("trade_status").asText())) {
                return Optional.empty();
            }
            String tradeNo = payload.path("trade_no").asText(null);
            return Optional.ofNullable(tradeNo == null || tradeNo.isBlank() ? payload.path("out_trade_no").asText(null) : tradeNo);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static boolean isUncreatedTrade(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return false;
        }
        try {
            JsonNode payload = MAPPER.readTree(rawBody).path("alipay_trade_query_response");
            return "ACQ.TRADE_NOT_EXIST".equals(payload.path("sub_code").asText());
        } catch (Exception e) {
            return false;
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 500 ? body : body.substring(0, 500);
    }

    public static Optional<Long> parsePaymentId(String outTradeNo, String passback) {
        if (outTradeNo != null && outTradeNo.startsWith(OUT_TRADE_PREFIX)) {
            try {
                return Optional.of(Long.parseLong(outTradeNo.substring(OUT_TRADE_PREFIX.length())));
            } catch (NumberFormatException ignored) {
                // fall through to passback
            }
        }
        if (passback != null && !passback.isBlank()) {
            try {
                return Optional.of(Long.parseLong(passback));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static boolean isPaidTradeStatus(String status) {
        return "TRADE_SUCCESS".equalsIgnoreCase(status) || "TRADE_FINISHED".equalsIgnoreCase(status);
    }

    private static Map<String, String> baseParams(PaymentProperties properties) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", properties.getAlipay().getAppId().trim());
        params.put("format", "JSON");
        params.put("charset", "UTF-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", ALIPAY_TIME.format(LocalDateTime.now()));
        params.put("version", "1.0");
        return params;
    }

    private static String resolveReturnUrl(PaymentProperties properties) {
        if (properties.getAlipay().getReturnUrl() != null && !properties.getAlipay().getReturnUrl().isBlank()) {
            return properties.getAlipay().getReturnUrl();
        }
        return "http://127.0.0.1:8080/api/v1/billing/payments/return/alipay";
    }

    private static String resolveNotifyUrl(PaymentProperties properties) {
        if (properties.getAlipay().getNotifyUrl() != null && !properties.getAlipay().getNotifyUrl().isBlank()) {
            return properties.getAlipay().getNotifyUrl();
        }
        return "http://127.0.0.1:8080/api/v1/billing/payments/notify/alipay";
    }

    static String sign(Map<String, String> params, String privateKeyPem) throws Exception {
        String content = signContent(params, false);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(readPrivateKey(privateKeyPem));
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    static boolean verifyNotify(Map<String, String> params, String publicKeyPem) {
        try {
            String sign = params.get("sign");
            if (sign == null || sign.isBlank() || publicKeyPem == null || publicKeyPem.isBlank()) {
                return false;
            }
            String content = signContent(params, true);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(readPublicKey(publicKeyPem));
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }

    private static String signContent(Map<String, String> params, boolean notifyVerify) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .filter(entry -> !"sign".equals(entry.getKey()))
                .filter(entry -> !notifyVerify || !"sign_type".equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    static PrivateKey readPrivateKey(String pem) throws Exception {
        byte[] decoded = decodePem(pem);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        try {
            return factory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (InvalidKeySpecException e) {
            return factory.generatePrivate(new PKCS8EncodedKeySpec(wrapPkcs1ToPkcs8(decoded)));
        }
    }

    static PublicKey readPublicKey(String pem) throws Exception {
        byte[] decoded = decodePem(pem);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private static byte[] decodePem(String pem) {
        String normalized = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    static byte[] wrapPkcs1ToPkcs8(byte[] pkcs1) {
        byte[] algorithmId = {
                0x30, 0x0d,
                0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01,
                0x05, 0x00
        };
        byte[] version = {0x02, 0x01, 0x00};
        byte[] octet = derTag((byte) 0x04, pkcs1);
        byte[] body = concat(version, algorithmId, octet);
        return derTag((byte) 0x30, body);
    }

    private static byte[] derTag(byte tag, byte[] content) {
        byte[] length = derLength(content.length);
        byte[] out = new byte[1 + length.length + content.length];
        out[0] = tag;
        System.arraycopy(length, 0, out, 1, length.length);
        System.arraycopy(content, 0, out, 1 + length.length, content.length);
        return out;
    }

    private static byte[] derLength(int length) {
        if (length < 128) {
            return new byte[]{(byte) length};
        }
        if (length < 256) {
            return new byte[]{(byte) 0x81, (byte) length};
        }
        return new byte[]{(byte) 0x82, (byte) ((length >> 8) & 0xff), (byte) (length & 0xff)};
    }

    private static byte[] concat(byte[]... parts) {
        int size = 0;
        for (byte[] part : parts) {
            size += part.length;
        }
        byte[] out = new byte[size];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, out, offset, part.length);
            offset += part.length;
        }
        return out;
    }

}
