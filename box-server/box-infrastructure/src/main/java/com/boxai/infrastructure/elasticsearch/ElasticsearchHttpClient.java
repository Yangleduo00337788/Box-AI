package com.boxai.infrastructure.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;

@Component
public class ElasticsearchHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchHttpClient.class);

    private final ElasticsearchProperties properties;
    private final HttpClient httpClient;

    public ElasticsearchHttpClient(ElasticsearchProperties properties) {
        this.properties = properties;
        this.httpClient = createHttpClient(properties);
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json");
        applyAuth(builder);
        if ("HEAD".equals(method)) {
            builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
        } else if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public boolean ping() {
        if (!properties.isEnabled()) {
            return false;
        }
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl()))
                    .timeout(Duration.ofSeconds(2))
                    .GET();
            applyAuth(builder);
            HttpResponse<Void> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    public String baseUrl() {
        String scheme = properties.getScheme() == null || properties.getScheme().isBlank()
                ? "http"
                : properties.getScheme().trim().toLowerCase();
        return scheme + "://" + properties.getHost() + ":" + properties.getPort();
    }

    private void applyAuth(HttpRequest.Builder builder) {
        String username = properties.getUsername();
        if (username == null || username.isBlank()) {
            return;
        }
        String password = properties.getPassword() == null ? "" : properties.getPassword();
        String token = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        builder.header("Authorization", "Basic " + token);
    }

    private static HttpClient createHttpClient(ElasticsearchProperties properties) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3));
        if (useInsecureTls(properties)) {
            try {
                TrustManager[] trustAll = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                };
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAll, new SecureRandom());
                builder.sslContext(sslContext);
            } catch (Exception ex) {
                log.warn("Failed to configure insecure Elasticsearch TLS, using default trust store: {}", ex.getMessage());
            }
        }
        return builder.build();
    }

    private static boolean useInsecureTls(ElasticsearchProperties properties) {
        String scheme = properties.getScheme();
        return "https".equalsIgnoreCase(scheme) && properties.isTrustInsecureCertificate();
    }
}
