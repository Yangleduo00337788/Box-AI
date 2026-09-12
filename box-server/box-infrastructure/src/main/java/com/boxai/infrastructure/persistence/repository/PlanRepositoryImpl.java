package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.plan.Plan;
import com.boxai.domain.plan.PlanRepository;
import com.boxai.infrastructure.persistence.entity.PlanDO;
import com.boxai.infrastructure.persistence.mapper.PlanMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PlanRepositoryImpl implements PlanRepository {

    private final PlanMapper planMapper;

    public PlanRepositoryImpl(PlanMapper planMapper) {
        this.planMapper = planMapper;
    }

    @Override
    public Plan save(Plan plan) {
        PlanDO row = toDo(plan);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        planMapper.insert(row);
        plan.setId(row.getId());
        plan.setCreatedAt(row.getCreatedAt());
        plan.setUpdatedAt(row.getUpdatedAt());
        return plan;
    }

    @Override
    public void update(Plan plan) {
        PlanDO row = toDo(plan);
        row.setId(plan.getId());
        row.setUpdatedAt(LocalDateTime.now());
        planMapper.update(row);
        plan.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<Plan> findById(Long id) {
        return Optional.ofNullable(planMapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Plan> findByCode(String code) {
        return Optional.ofNullable(
                planMapper.selectOneByQuery(QueryWrapper.create().eq("code", code)))
                .map(this::toDomain);
    }

    @Override
    public List<Plan> listAll() {
        return planMapper.selectListByQuery(QueryWrapper.create().orderBy("price_monthly", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Plan toDomain(PlanDO row) {
        Plan plan = new Plan();
        plan.setId(row.getId());
        plan.setCode(row.getCode());
        plan.setName(row.getName());
        plan.setDescription(row.getDescription());
        plan.setPriceMonthly(row.getPriceMonthly());
        plan.setQuotaAiCalls(row.getQuotaAiCalls());
        plan.setQuotaTokens(row.getQuotaTokens());
        plan.setQuotaMembers(row.getQuotaMembers());
        plan.setQuotaWorkspaces(row.getQuotaWorkspaces());
        plan.setQuotaKnowledgeBases(row.getQuotaKnowledgeBases());
        plan.setByokEnabled(row.getByokEnabled());
        plan.setStatus(row.getStatus());
        plan.setCreatedAt(row.getCreatedAt());
        plan.setUpdatedAt(row.getUpdatedAt());
        return plan;
    }

    private PlanDO toDo(Plan plan) {
        PlanDO row = new PlanDO();
        row.setCode(plan.getCode());
        row.setName(plan.getName());
        row.setDescription(plan.getDescription());
        row.setPriceMonthly(plan.getPriceMonthly());
        row.setQuotaAiCalls(plan.getQuotaAiCalls());
        row.setQuotaTokens(plan.getQuotaTokens());
        row.setQuotaMembers(plan.getQuotaMembers());
        row.setQuotaWorkspaces(plan.getQuotaWorkspaces());
        row.setQuotaKnowledgeBases(plan.getQuotaKnowledgeBases());
        row.setByokEnabled(plan.getByokEnabled() == null ? 0 : plan.getByokEnabled());
        row.setStatus(plan.getStatus() == null ? 1 : plan.getStatus());
        return row;
    }
}
