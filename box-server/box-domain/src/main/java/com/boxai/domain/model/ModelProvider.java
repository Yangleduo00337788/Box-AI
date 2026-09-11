package com.boxai.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelProvider {

    private Long id;
    private Long workspaceId;
    private String providerCode;
    private String providerName;
    private String providerType;
    private String baseUrl;
    private Integer status;
    private String configJson;
}
