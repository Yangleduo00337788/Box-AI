package com.boxai.domain.tenant;

import java.util.Optional;

public interface TenantOAuthOrgRepository {

    Optional<TenantOAuthOrg> findByProviderAndOrgId(String provider, String orgId);

    TenantOAuthOrg save(TenantOAuthOrg binding);
}
