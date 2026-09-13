package com.boxai.domain.publish;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmbedCustomDomain {

    private Long id;
    private Long workspaceId;
    private Long agentId;
    private String domain;
    private String verifyToken;
    private Integer verified;
}
