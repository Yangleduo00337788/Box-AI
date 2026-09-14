package com.boxai.domain.plan;

import java.util.List;
import java.util.Optional;

public interface TenantUsageRepository {

    Optional<TenantUsage> findByTenantAndPeriod(Long tenantId, String period);

    List<TenantUsage> listByPeriod(String period);

    TenantUsage save(TenantUsage usage);

    void update(TenantUsage usage);
}
