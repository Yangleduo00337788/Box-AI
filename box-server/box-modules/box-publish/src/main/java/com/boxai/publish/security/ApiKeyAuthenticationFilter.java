package com.boxai.publish.security;

import com.boxai.common.constant.ApiKeyPrefixes;
import com.boxai.common.constant.HeaderNames;
import com.boxai.common.constant.RequestAttributes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.result.Result;
import com.boxai.domain.apikey.ApiKey;
import com.boxai.publish.application.ApiKeyApplicationService;
import com.boxai.security.context.LoginUser;
import com.boxai.security.ratelimit.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyApplicationService apiKeyApplicationService;
    private final ObjectMapper objectMapper;
    private final RateLimitService rateLimitService;

    public ApiKeyAuthenticationFilter(ApiKeyApplicationService apiKeyApplicationService,
                                    ObjectMapper objectMapper,
                                    RateLimitService rateLimitService) {
        this.apiKeyApplicationService = apiKeyApplicationService;
        this.objectMapper = objectMapper;
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/v1/published/")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod())
                && (uri.equals("/api/v1/published/embed/resolve")
                || uri.matches("/api/v1/published/agents/\\d+/embed-config"))) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        String header = request.getHeader(HeaderNames.AUTHORIZATION);
        if (header == null || !header.startsWith(HeaderNames.BEARER_PREFIX)) {
            writeUnauthorized(response, "缺少 API Key");
            return;
        }
        String token = header.substring(HeaderNames.BEARER_PREFIX.length()).trim();
        if (!token.startsWith(ApiKeyPrefixes.LIVE)) {
            writeUnauthorized(response, "API Key 格式无效");
            return;
        }
        try {
            ApiKey apiKey = apiKeyApplicationService.authenticate(token);
            rateLimitService.assertAllowed(
                    "api-key",
                    String.valueOf(apiKey.getId()),
                    120,
                    Duration.ofMinutes(1));
            request.setAttribute(RequestAttributes.API_KEY_ID, apiKey.getId());
            request.setAttribute(RequestAttributes.API_KEY_WORKSPACE_ID, apiKey.getWorkspaceId());
            request.setAttribute(RequestAttributes.API_KEY_USER_ID, apiKey.getCreatedBy());
            var authentication = new UsernamePasswordAuthenticationToken(
                    new LoginUser(apiKey.getCreatedBy(), "api-key", "API_KEY"),
                    null,
                    List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            apiKeyApplicationService.touchLastUsed(apiKey);
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            if (ex.getCode() == ErrorCode.TOO_MANY_REQUESTS) {
                writeTooManyRequests(response, ex.getMessage());
                return;
            }
            writeUnauthorized(response, ex.getMessage());
        }
    }

    private void writeTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.failure(ErrorCode.TOO_MANY_REQUESTS, message)));
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.failure(ErrorCode.UNAUTHORIZED, message)));
    }
}
