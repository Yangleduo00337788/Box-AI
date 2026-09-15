package com.boxai.domain.workspace;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Workspace {

    private Long id;
    private Long tenantId;
    private String name;
    private String slug;
    private String description;
    private String avatarUrl;
    private Long ownerId;
    private Integer status;
    private LocalDateTime createdAt;
}
