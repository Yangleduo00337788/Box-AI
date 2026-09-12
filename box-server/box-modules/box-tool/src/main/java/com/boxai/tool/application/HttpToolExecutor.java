package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.SsrfGuard;
import com.boxai.common.security.SsrfSafeHttpClient;
import com.boxai.domain.tool.ToolHttpConfig;
import com.boxai.tool.api.ToolTestResultVO;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class HttpToolExecutor {

    public ToolTestResultVO execute(ToolHttpConfig config) {
        long started = System.currentTimeMillis();
        try {
            Duration timeout = Duration.ofMillis(config.getTimeoutMs() == null ? 10000 : config.getTimeoutMs());
            HttpClient client = SsrfSafeHttpClient.create(timeout, Boolean.TRUE.equals(config.getAllowRedirect()));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(SsrfGuard.validateHttpUrl(config.getUrl()))
                    .timeout(timeout);
            String method = config.getMethod() == null ? "GET" : config.getMethod().toUpperCase();
            if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
                String body = config.getBodyTemplate() == null ? "" : config.getBodyTemplate();
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = SsrfSafeHttpClient.send(
                    client,
                    builder.build(),
                    Boolean.TRUE.equals(config.getAllowRedirect()),
                    timeout);
            return new ToolTestResultVO(
                    response.statusCode(),
                    response.body(),
                    System.currentTimeMillis() - started);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "HTTP 工具调用失败: " + e.getMessage());
        }
    }
}
