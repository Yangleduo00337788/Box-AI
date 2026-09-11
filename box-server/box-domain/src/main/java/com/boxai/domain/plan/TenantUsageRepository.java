package com.boxai.domain.plan;

import java.util.Optional;

public interface TenantUsageRepository {

    Optional<TenantUsage> findByTenantAndPeriod(Long tenantId, String period);

    TenantUsage save(TenantUsage usage);

    void update(TenantUsage usage);
}
