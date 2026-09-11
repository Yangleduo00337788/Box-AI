package com.boxai.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "box.security.jwt")
public class JwtProperties {

    private String secret = "box-dev-jwt-secret-change-me-please-32b";
    private long expireSeconds = 86400;
}
