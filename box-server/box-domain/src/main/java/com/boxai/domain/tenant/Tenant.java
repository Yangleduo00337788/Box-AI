package com.boxai.domain.tenant;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Tenant {

    private Long id;
    private String name;
    private String slug;
    private String tenantType;
    private Long planId;
    private String contactEmail;
    private Integer status;
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
