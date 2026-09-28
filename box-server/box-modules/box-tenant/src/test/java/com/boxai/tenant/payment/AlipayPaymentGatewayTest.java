package com.boxai.tenant.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlipayPaymentGatewayTest {

    private final AlipayPaymentGateway gateway = new AlipayPaymentGateway();

    @Test
    void supportsAlipayWhenAppAndKeyPresent() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("alipay");
        properties.getAlipay().setAppId("sandbox-app");
        properties.getAlipay().setPrivateKey("not-empty");
        assertTrue(gateway.supports(properties));
    }

    @Test
    void signsAndVerifiesNotify() throws Exception {
        KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String privatePem = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        String publicPem = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        Map<String, String> params = new TreeMap<>();
        params.put("app_id", "sandbox");
        params.put("out_trade_no", "BOX-PAY-42");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("trade_no", "20260101001");
        params.put("sign", AlipayPaymentGateway.sign(params, privatePem));
        params.put("sign_type", "RSA2");

        assertTrue(AlipayPaymentGateway.verifyNotify(params, publicPem));
        PaymentProperties properties = new PaymentProperties();
        properties.getAlipay().setAlipayPublicKey(publicPem);
        assertEquals(Optional.of(42L), gateway.resolvePaymentIdFromNotify(params, properties));
    }

    @Test
    void parsesPaidQueryResponse() {
        String body = """
                {"alipay_trade_query_response":{"code":"10000","trade_status":"TRADE_SUCCESS","trade_no":"T1","out_trade_no":"BOX-PAY-9"}}
                """;
        assertEquals(Optional.of("T1"), AlipayPaymentGateway.parsePaidTradeNo(body));
    }

    @Test
    void recognizesUncreatedTrade() {
        String body = """
                {"alipay_trade_query_response":{"code":"40004","sub_code":"ACQ.TRADE_NOT_EXIST","out_trade_no":"BOX-PAY-8"}}
                """;
        assertTrue(AlipayPaymentGateway.isUncreatedTrade(body));
        assertTrue(AlipayPaymentGateway.parsePaidTradeNo(body).isEmpty());
    }

    @Test
    void parsePaymentIdFallsBackToPassback() {
        assertEquals(Optional.of(7L), AlipayPaymentGateway.parsePaymentId("bad", "7"));
        assertEquals(Optional.of(8L), AlipayPaymentGateway.parsePaymentId("BOX-PAY-8", null));
    }

    @Test
    void checkoutIncludesTenMinuteTimeout() throws Exception {
        KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        PaymentProperties properties = new PaymentProperties();
        properties.getAlipay().setAppId("sandbox-app");
        properties.getAlipay().setPrivateKey(Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
        properties.getAlipay().setGatewayUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");

        PaymentCheckoutResult result = gateway.createCheckout(
                new PaymentCheckoutCommand(8L, 1L, 2L, "INV-1", new BigDecimal("299.00"), "CNY", "团队商业"),
                properties);

        String biz = result.checkoutForm().get("biz_content");
        assertTrue(biz.contains("\"timeout_express\":\"10m\""));
        assertTrue(biz.contains("time_expire"));
    }
}
