package com.boxai.domain.rbac;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Permission {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String resourceType;
    private String action;
}
