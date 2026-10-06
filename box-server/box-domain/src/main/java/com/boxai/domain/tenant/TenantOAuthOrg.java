package com.boxai.domain.tenant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantOAuthOrg {

    private Long id;
    private Long tenantId;
    private String provider;
    private String orgId;
}
