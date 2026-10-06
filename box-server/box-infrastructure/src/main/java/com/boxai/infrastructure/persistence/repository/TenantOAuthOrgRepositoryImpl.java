package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.tenant.TenantOAuthOrg;
import com.boxai.domain.tenant.TenantOAuthOrgRepository;
import com.boxai.infrastructure.persistence.entity.TenantOAuthOrgDO;
import com.boxai.infrastructure.persistence.mapper.TenantOAuthOrgMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class TenantOAuthOrgRepositoryImpl implements TenantOAuthOrgRepository {

    private final TenantOAuthOrgMapper mapper;

    public TenantOAuthOrgRepositoryImpl(TenantOAuthOrgMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<TenantOAuthOrg> findByProviderAndOrgId(String provider, String orgId) {
        TenantOAuthOrgDO row = mapper.selectOneByQuery(QueryWrapper.create()
                .eq("provider", provider)
                .eq("org_id", orgId));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public TenantOAuthOrg save(TenantOAuthOrg binding) {
        TenantOAuthOrgDO row = new TenantOAuthOrgDO();
        row.setTenantId(binding.getTenantId());
        row.setProvider(binding.getProvider());
        row.setOrgId(binding.getOrgId());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(row.getCreatedAt());
        mapper.insert(row);
        binding.setId(row.getId());
        return binding;
    }

    private TenantOAuthOrg toDomain(TenantOAuthOrgDO row) {
        TenantOAuthOrg binding = new TenantOAuthOrg();
        binding.setId(row.getId());
        binding.setTenantId(row.getTenantId());
        binding.setProvider(row.getProvider());
        binding.setOrgId(row.getOrgId());
        return binding;
    }
}
