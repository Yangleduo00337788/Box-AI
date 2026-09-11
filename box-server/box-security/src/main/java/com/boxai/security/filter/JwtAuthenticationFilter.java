package com.boxai.security.filter;

import com.boxai.common.constant.HeaderNames;
import com.boxai.common.constant.UserTypes;
import com.boxai.security.context.LoginUser;
import com.boxai.security.jwt.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HeaderNames.AUTHORIZATION);
        if (header != null && header.startsWith(HeaderNames.BEARER_PREFIX)) {
            String token = header.substring(HeaderNames.BEARER_PREFIX.length());
            try {
                var claims = jwtService.parse(token);
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get("username", String.class);
                String userType = claims.get("userType", String.class);
                if (userType == null || userType.isBlank()) {
                    userType = UserTypes.TENANT_USER;
                }
                var authentication = new UsernamePasswordAuthenticationToken(
                        new LoginUser(userId, username, userType), null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
