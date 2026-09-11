package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.tool.ToolHttpConfig;
import com.boxai.tool.api.ToolTestResultVO;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class HttpToolExecutor {

    public ToolTestResultVO execute(ToolHttpConfig config) {
        long started = System.currentTimeMillis();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(config.getTimeoutMs() == null ? 10000 : config.getTimeoutMs()))
                    .followRedirects(Boolean.TRUE.equals(config.getAllowRedirect())
                            ? HttpClient.Redirect.NORMAL
                            : HttpClient.Redirect.NEVER)
                    .build();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(config.getUrl()))
                    .timeout(Duration.ofMillis(config.getTimeoutMs() == null ? 10000 : config.getTimeoutMs()));
            String method = config.getMethod() == null ? "GET" : config.getMethod().toUpperCase();
            if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
                String body = config.getBodyTemplate() == null ? "" : config.getBodyTemplate();
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new ToolTestResultVO(
                    response.statusCode(),
                    response.body(),
                    System.currentTimeMillis() - started);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "HTTP 工具调用失败: " + e.getMessage());
        }
    }
}
