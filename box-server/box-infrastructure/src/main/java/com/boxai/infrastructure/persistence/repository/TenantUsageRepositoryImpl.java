package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.plan.TenantUsage;
import com.boxai.domain.plan.TenantUsageRepository;
import com.boxai.infrastructure.persistence.entity.TenantUsageDO;
import com.boxai.infrastructure.persistence.mapper.TenantUsageMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TenantUsageRepositoryImpl implements TenantUsageRepository {

    private final TenantUsageMapper tenantUsageMapper;

    public TenantUsageRepositoryImpl(TenantUsageMapper tenantUsageMapper) {
        this.tenantUsageMapper = tenantUsageMapper;
    }

    @Override
    public Optional<TenantUsage> findByTenantAndPeriod(Long tenantId, String period) {
        return Optional.ofNullable(tenantUsageMapper.selectOneByQuery(
                        QueryWrapper.create().eq("tenant_id", tenantId).eq("period", period)))
                .map(this::toDomain);
    }

    @Override
    public List<TenantUsage> listByPeriod(String period) {
        return tenantUsageMapper.selectListByQuery(QueryWrapper.create().eq("period", period)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public TenantUsage save(TenantUsage usage) {
        TenantUsageDO row = toDo(usage);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        tenantUsageMapper.insert(row);
        usage.setId(row.getId());
        usage.setCreatedAt(row.getCreatedAt());
        usage.setUpdatedAt(row.getUpdatedAt());
        return usage;
    }

    @Override
    public void update(TenantUsage usage) {
        TenantUsageDO row = toDo(usage);
        row.setId(usage.getId());
        row.setUpdatedAt(LocalDateTime.now());
        tenantUsageMapper.update(row);
        usage.setUpdatedAt(row.getUpdatedAt());
    }

    private TenantUsage toDomain(TenantUsageDO row) {
        TenantUsage usage = new TenantUsage();
        usage.setId(row.getId());
        usage.setTenantId(row.getTenantId());
        usage.setPeriod(row.getPeriod());
        usage.setAiCalls(row.getAiCalls());
        usage.setTokens(row.getTokens());
        usage.setOverageAiCalls(row.getOverageAiCalls());
        usage.setOverageTokens(row.getOverageTokens());
        usage.setCreatedAt(row.getCreatedAt());
        usage.setUpdatedAt(row.getUpdatedAt());
        return usage;
    }

    private TenantUsageDO toDo(TenantUsage usage) {
        TenantUsageDO row = new TenantUsageDO();
        row.setTenantId(usage.getTenantId());
        row.setPeriod(usage.getPeriod());
        row.setAiCalls(usage.getAiCalls() == null ? 0 : usage.getAiCalls());
        row.setTokens(usage.getTokens() == null ? 0L : usage.getTokens());
        row.setOverageAiCalls(usage.getOverageAiCalls() == null ? 0 : usage.getOverageAiCalls());
        row.setOverageTokens(usage.getOverageTokens() == null ? 0L : usage.getOverageTokens());
        return row;
    }
}
