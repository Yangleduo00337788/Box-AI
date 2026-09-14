package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantMember;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.infrastructure.persistence.entity.TenantDO;
import com.boxai.infrastructure.persistence.entity.TenantMemberDO;
import com.boxai.infrastructure.persistence.mapper.TenantMapper;
import com.boxai.infrastructure.persistence.mapper.TenantMemberMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TenantRepositoryImpl implements TenantRepository {

    private final TenantMapper tenantMapper;
    private final TenantMemberMapper memberMapper;

    public TenantRepositoryImpl(TenantMapper tenantMapper, TenantMemberMapper memberMapper) {
        this.tenantMapper = tenantMapper;
        this.memberMapper = memberMapper;
    }

    @Override
    public Tenant save(Tenant tenant) {
        TenantDO row = toDo(tenant);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        tenantMapper.insert(row);
        tenant.setId(row.getId());
        tenant.setCreatedAt(row.getCreatedAt());
        tenant.setUpdatedAt(row.getUpdatedAt());
        return tenant;
    }

    @Override
    public void update(Tenant tenant) {
        TenantDO row = toDo(tenant);
        row.setId(tenant.getId());
        row.setUpdatedAt(LocalDateTime.now());
        tenantMapper.update(row);
        tenant.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return Optional.ofNullable(tenantMapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return Optional.ofNullable(
                tenantMapper.selectOneByQuery(QueryWrapper.create().eq("slug", slug)))
                .map(this::toDomain);
    }

    @Override
    public List<Tenant> listAll() {
        return tenantMapper.selectListByQuery(QueryWrapper.create().orderBy("created_at", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByPlanId(Long planId) {
        if (planId == null) {
            return 0;
        }
        return tenantMapper.selectCountByQuery(QueryWrapper.create().eq("plan_id", planId));
    }

    @Override
    public Optional<TenantMember> findMember(Long tenantId, Long userId) {
        return Optional.ofNullable(memberMapper.selectOneByQuery(
                        QueryWrapper.create().eq("tenant_id", tenantId).eq("user_id", userId)))
                .map(this::toMember);
    }

    @Override
    public Optional<TenantMember> findPrimaryByUserId(Long userId) {
        return memberMapper.selectListByQuery(
                        QueryWrapper.create().eq("user_id", userId).eq("status", 1).orderBy("joined_at", true).limit(1))
                .stream()
                .findFirst()
                .map(this::toMember);
    }

    @Override
    public List<TenantMember> listMembersByTenantId(Long tenantId) {
        return memberMapper.selectListByQuery(
                        QueryWrapper.create().eq("tenant_id", tenantId).orderBy("joined_at", true))
                .stream()
                .map(this::toMember)
                .toList();
    }

    @Override
    public TenantMember addMember(TenantMember member) {
        TenantMemberDO row = new TenantMemberDO();
        row.setTenantId(member.getTenantId());
        row.setUserId(member.getUserId());
        row.setRoleCode(member.getRoleCode());
        row.setStatus(member.getStatus() == null ? 1 : member.getStatus());
        row.setJoinedAt(LocalDateTime.now());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        memberMapper.insert(row);
        member.setId(row.getId());
        member.setJoinedAt(row.getJoinedAt());
        return member;
    }

    @Override
    public void updateMember(TenantMember member) {
        TenantMemberDO row = new TenantMemberDO();
        row.setId(member.getId());
        row.setRoleCode(member.getRoleCode());
        row.setStatus(member.getStatus());
        row.setUpdatedAt(LocalDateTime.now());
        memberMapper.update(row);
    }

    private Tenant toDomain(TenantDO row) {
        Tenant tenant = new Tenant();
        tenant.setId(row.getId());
        tenant.setName(row.getName());
        tenant.setSlug(row.getSlug());
        tenant.setTenantType(row.getTenantType());
        tenant.setPlanId(row.getPlanId());
        tenant.setContactEmail(row.getContactEmail());
        tenant.setStatus(row.getStatus());
        tenant.setOwnerId(row.getOwnerId());
        tenant.setCreatedAt(row.getCreatedAt());
        tenant.setUpdatedAt(row.getUpdatedAt());
        return tenant;
    }

    private TenantDO toDo(Tenant tenant) {
        TenantDO row = new TenantDO();
        row.setName(tenant.getName());
        row.setSlug(tenant.getSlug());
        row.setTenantType(tenant.getTenantType());
        row.setPlanId(tenant.getPlanId());
        row.setContactEmail(tenant.getContactEmail());
        row.setStatus(tenant.getStatus() == null ? 1 : tenant.getStatus());
        row.setOwnerId(tenant.getOwnerId());
        return row;
    }

    private TenantMember toMember(TenantMemberDO row) {
        TenantMember member = new TenantMember();
        member.setId(row.getId());
        member.setTenantId(row.getTenantId());
        member.setUserId(row.getUserId());
        member.setRoleCode(row.getRoleCode());
        member.setStatus(row.getStatus());
        member.setJoinedAt(row.getJoinedAt());
        return member;
    }
}
