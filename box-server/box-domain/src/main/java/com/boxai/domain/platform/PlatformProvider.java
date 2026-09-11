package com.boxai.domain.platform;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PlatformProvider {

    private Long id;
    private String providerCode;
    private String providerName;
    private String providerType;
    private String baseUrl;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
