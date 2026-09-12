package com.boxai.security.filter;

import com.boxai.common.constant.HeaderNames;
import com.boxai.common.constant.RequestAttributes;
import com.boxai.security.logging.LoggingContext;
import com.boxai.security.request.RequestIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = RequestIdGenerator.resolve(request.getHeader(HeaderNames.REQUEST_ID));
        request.setAttribute(RequestAttributes.REQUEST_ID, requestId);
        response.setHeader(HeaderNames.REQUEST_ID, requestId);
        LoggingContext.setRequestId(requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            LoggingContext.clear();
        }
    }
}
