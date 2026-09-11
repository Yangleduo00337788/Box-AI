package com.boxai.domain.workspace;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkspaceMember {

    private Long id;
    private Long workspaceId;
    private Long userId;
    private Long roleId;
    private Integer status;
    private String roleCode;
    private String workspaceName;
    private String workspaceSlug;
}
