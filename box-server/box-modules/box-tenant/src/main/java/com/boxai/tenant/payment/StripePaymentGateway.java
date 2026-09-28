package com.boxai.tenant.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StripePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long WEBHOOK_TOLERANCE_SECONDS = 300L;
    private static final Set<String> ZERO_DECIMAL = Set.of(
            "bif", "clp", "djf", "gnf", "jpy", "kmf", "krw", "mga", "pyg", "rwf", "ugx", "vnd", "vuv", "xaf", "xof", "xpf");

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public String channel() {
        return "STRIPE";
    }

    @Override
    public boolean supports(PaymentProperties properties) {
        return "stripe".equalsIgnoreCase(properties.getProvider())
                && properties.getStripe().getSecretKey() != null
                && !properties.getStripe().getSecretKey().isBlank();
    }

    @Override
    public PaymentCheckoutResult createCheckout(PaymentCheckoutCommand command, PaymentProperties properties) {
        try {
            String currency = properties.getStripe().checkoutCurrency(command.currency());
            long amountMinor = toStripeAmount(command.amount(), currency);
            String successUrl = PaymentUrls.withQuery(
                    properties.getSuccessUrl(), "paymentId", String.valueOf(command.paymentId()));
            String cancelUrl = PaymentUrls.withQuery(
                    properties.getCancelUrl(), "paymentId", String.valueOf(command.paymentId()));
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("mode", "payment");
            fields.put("success_url", successUrl + "&session_id={CHECKOUT_SESSION_ID}");
            fields.put("cancel_url", cancelUrl);
            fields.put("client_reference_id", String.valueOf(command.paymentId()));
            fields.put("metadata[payment_id]", String.valueOf(command.paymentId()));
            fields.put("metadata[invoice_id]", String.valueOf(command.invoiceId()));
            fields.put("line_items[0][price_data][currency]", currency);
            fields.put("line_items[0][price_data][product_data][name]", command.subject());
            fields.put("line_items[0][price_data][unit_amount]", String.valueOf(amountMinor));
            fields.put("line_items[0][quantity]", "1");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.stripe.com/v1/checkout/sessions"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + properties.getStripe().getSecretKey())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(buildForm(fields)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Stripe 创建会话失败: " + response.body());
            }
            JsonNode json = MAPPER.readTree(response.body());
            String sessionId = json.path("id").asText();
            String url = json.path("url").asText();
            return new PaymentCheckoutResult(channel(), url, sessionId, false);
        } catch (Exception e) {
            throw new IllegalStateException("Stripe 支付初始化失败", e);
        }
    }

    public boolean isWebhookAuthentic(String rawBody, Map<String, String> headers, PaymentProperties properties) {
        String webhookSecret = properties.getStripe().getWebhookSecret();
        if (rawBody == null || webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }
        String signature = header(headers, "Stripe-Signature");
        return signature != null && !signature.isBlank()
                && verifyStripeSignature(rawBody, signature, webhookSecret);
    }

    @Override
    public Optional<Long> resolvePaymentIdFromWebhook(String rawBody, Map<String, String> headers, PaymentProperties properties) {
        if (!isWebhookAuthentic(rawBody, headers, properties)) {
            return Optional.empty();
        }
        try {
            JsonNode event = MAPPER.readTree(rawBody);
            if (!"checkout.session.completed".equals(event.path("type").asText())) {
                return Optional.empty();
            }
            JsonNode session = event.path("data").path("object");
            if (!isPaidSession(session)) {
                return Optional.empty();
            }
            if (session.hasNonNull("metadata") && session.get("metadata").hasNonNull("payment_id")) {
                return Optional.of(session.get("metadata").get("payment_id").asLong());
            }
            String clientRef = session.path("client_reference_id").asText(null);
            if (clientRef != null && !clientRef.isBlank()) {
                return Optional.of(Long.parseLong(clientRef));
            }
        } catch (Exception ex) {
            log.warn("Failed to parse Stripe webhook payment id: {}", ex.getMessage());
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Long> resolvePaymentIdFromNotify(Map<String, String> params, PaymentProperties properties) {
        return Optional.empty();
    }

    @Override
    public Optional<String> queryPaidExternalRef(String externalRef, Long paymentId, PaymentProperties properties) {
        if (externalRef == null || !externalRef.startsWith("cs_")
                || properties.getStripe().getSecretKey() == null
                || properties.getStripe().getSecretKey().isBlank()) {
            return Optional.empty();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.stripe.com/v1/checkout/sessions/" + externalRef))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + properties.getStripe().getSecretKey())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Stripe 查单失败: HTTP {}", response.statusCode());
                return Optional.empty();
            }
            JsonNode session = MAPPER.readTree(response.body());
            if (!isPaidSession(session)) {
                return Optional.empty();
            }
            String id = session.path("id").asText(null);
            return Optional.ofNullable(id == null || id.isBlank() ? externalRef : id);
        } catch (Exception e) {
            log.warn("Stripe 查单异常: {}", e.getMessage());
            return Optional.empty();
        }
    }

    static long toStripeAmount(BigDecimal amount, String currency) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        if (ZERO_DECIMAL.contains(currency == null ? "" : currency.toLowerCase(Locale.ROOT))) {
            return value.setScale(0, RoundingMode.HALF_UP).longValue();
        }
        return value.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    static boolean isPaidSession(JsonNode session) {
        if (session == null || session.isMissingNode()) {
            return false;
        }
        String status = session.path("payment_status").asText("");
        return "paid".equalsIgnoreCase(status) || status.isBlank();
    }

    static boolean verifyStripeSignature(String payload, String signatureHeader, String secret) {
        try {
            String timestamp = null;
            String signature = null;
            for (String part : signatureHeader.split(",")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length != 2) {
                    continue;
                }
                if ("t".equals(kv[0])) {
                    timestamp = kv[1];
                } else if ("v1".equals(kv[0])) {
                    signature = kv[1];
                }
            }
            if (timestamp == null || signature == null) {
                return false;
            }
            long eventSeconds = Long.parseLong(timestamp);
            long nowSeconds = System.currentTimeMillis() / 1000L;
            if (Math.abs(nowSeconds - eventSeconds) > WEBHOOK_TOLERANCE_SECONDS) {
                return false;
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest).equalsIgnoreCase(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private static String buildForm(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(entry -> PaymentUrls.encode(entry.getKey()) + "=" + PaymentUrls.encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String header(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
