package com.boxai.domain.rbac;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Role {

    private Long id;
    private Long workspaceId;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer builtIn;
}
