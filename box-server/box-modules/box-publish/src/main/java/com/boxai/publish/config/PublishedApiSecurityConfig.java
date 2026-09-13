package com.boxai.publish.config;

import com.boxai.publish.security.ApiKeyAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class PublishedApiSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain publishedApiSecurityFilterChain(HttpSecurity http,
                                                               ApiKeyAuthenticationFilter apiKeyAuthenticationFilter)
            throws Exception {
        http.securityMatcher("/api/v1/published/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/published/agents/*/embed-config").permitAll()
                        .requestMatchers("/api/v1/published/embed/resolve").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
