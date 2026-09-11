package com.boxai.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelCredential {

    private Long id;
    private Long workspaceId;
    private Long providerId;
    private String credentialName;
    private String encryptedApiKey;
    private Integer status;
}
