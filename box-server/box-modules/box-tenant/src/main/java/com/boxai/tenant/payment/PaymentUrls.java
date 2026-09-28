package com.boxai.tenant.payment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class PaymentUrls {

    private PaymentUrls() {
    }

    public static String withQuery(String url, String name, String value) {
        if (url == null || url.isBlank() || value == null) {
            return url;
        }
        String join = url.contains("?") ? "&" : "?";
        return url + join + encode(name) + "=" + encode(value);
    }

    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
