package com.boxai.infrastructure.crypto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "box.security.crypto")
public class CryptoProperties {

    private String aesKey = "box-dev-aes-256-key-change-me!!";
}
