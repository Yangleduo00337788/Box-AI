package com.boxai.common.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.BiConsumer;

public final class SsrfSafeHttpClient {

    private static final int MAX_REDIRECTS = 3;

    private SsrfSafeHttpClient() {
    }

    public static HttpClient create(Duration connectTimeout, boolean allowRedirect) {
        return HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public static HttpResponse<String> send(HttpClient client,
                                            HttpRequest request,
                                            boolean allowRedirect,
                                            Duration timeout) throws Exception {
        return send(client, request, allowRedirect, timeout, null);
    }

    public static HttpResponse<String> send(HttpClient client,
                                            HttpRequest request,
                                            boolean allowRedirect,
                                            Duration timeout,
                                            BiConsumer<Integer, URI> redirectHook) throws Exception {
        HttpRequest current = request;
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            HttpResponse<String> response = client.send(current, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (!allowRedirect || status < 300 || status >= 400) {
                return response;
            }
            String location = response.headers().firstValue("Location").orElse(null);
            if (location == null || location.isBlank()) {
                return response;
            }
            URI nextUri = SsrfGuard.validateHttpUrl(location);
            if (redirectHook != null) {
                redirectHook.accept(status, nextUri);
            }
            current = HttpRequest.newBuilder(current, (name, value) -> true)
                    .uri(nextUri)
                    .timeout(timeout)
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
        }
        throw new IllegalStateException("HTTP 重定向次数过多");
    }
}
