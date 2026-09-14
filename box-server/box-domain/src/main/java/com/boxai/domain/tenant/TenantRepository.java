package com.boxai.domain.tenant;

import java.util.List;
import java.util.Optional;

public interface TenantRepository {

    Tenant save(Tenant tenant);

    void update(Tenant tenant);

    Optional<Tenant> findById(Long id);

    Optional<Tenant> findBySlug(String slug);

    List<Tenant> listAll();

    long countByPlanId(Long planId);

    Optional<TenantMember> findMember(Long tenantId, Long userId);

    Optional<TenantMember> findPrimaryByUserId(Long userId);

    List<TenantMember> listMembersByTenantId(Long tenantId);

    TenantMember addMember(TenantMember member);

    void updateMember(TenantMember member);
}
