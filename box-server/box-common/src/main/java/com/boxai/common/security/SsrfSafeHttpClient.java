package com.boxai.common.security;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
        return send(client, request, allowRedirect, timeout, HttpResponse.BodyHandlers.ofString(), null);
    }

    public static HttpResponse<String> send(HttpClient client,
                                            HttpRequest request,
                                            boolean allowRedirect,
                                            Duration timeout,
                                            BiConsumer<Integer, URI> redirectHook) throws Exception {
        return send(client, request, allowRedirect, timeout, HttpResponse.BodyHandlers.ofString(), redirectHook);
    }

    public static <T> HttpResponse<T> send(HttpClient client,
                                           HttpRequest request,
                                           boolean allowRedirect,
                                           Duration timeout,
                                           HttpResponse.BodyHandler<T> handler,
                                           BiConsumer<Integer, URI> redirectHook) throws Exception {
        HttpRequest current = request;
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            HttpResponse<T> response = client.send(current, handler);
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

    public static byte[] getBytes(String rawUrl, Duration timeout, long maxBytes) throws Exception {
        URI uri = SsrfGuard.validateHttpUrl(rawUrl);
        HttpClient client = create(timeout, false);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(timeout)
                .GET()
                .build();
        HttpResponse<InputStream> response = send(
                client, request, true, timeout, HttpResponse.BodyHandlers.ofInputStream(), null);
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "抓取网页失败: HTTP " + status);
        }
        long declared = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        if (declared > maxBytes) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "网页内容不能超过 20MB");
        }
        try (InputStream in = response.body()) {
            return readLimited(in, maxBytes);
        }
    }

    private static byte[] readLimited(InputStream in, long maxBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) >= 0) {
            total += read;
            if (total > maxBytes) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "网页内容不能超过 20MB");
            }
            out.write(buffer, 0, read);
        }
        if (total == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "网页内容为空");
        }
        return out.toByteArray();
    }
}
