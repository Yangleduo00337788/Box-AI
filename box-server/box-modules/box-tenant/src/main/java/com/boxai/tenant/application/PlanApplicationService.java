package com.boxai.tenant.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plan.Plan;
import com.boxai.domain.plan.PlanRepository;
import com.boxai.tenant.api.CreatePlanRequest;
import com.boxai.tenant.api.PlanVO;
import com.boxai.tenant.api.UpdatePlanRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class PlanApplicationService {

    private final PlanRepository planRepository;

    public PlanApplicationService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<PlanVO> listAll() {
        return planRepository.listAll().stream().map(this::toVo).toList();
    }

    public PlanVO detail(Long id) {
        return toVo(requirePlan(id));
    }

    @Transactional
    public PlanVO create(CreatePlanRequest request) {
        String code = request.code().trim().toLowerCase(Locale.ROOT);
        if (planRepository.findByCode(code).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "套餐编码已存在");
        }
        Plan plan = new Plan();
        plan.setCode(code);
        plan.setName(request.name().trim());
        plan.setDescription(trimToNull(request.description()));
        plan.setPriceMonthly(request.priceMonthly());
        plan.setQuotaAiCalls(request.quotaAiCalls());
        plan.setQuotaTokens(request.quotaTokens());
        plan.setQuotaMembers(request.quotaMembers());
        plan.setQuotaWorkspaces(request.quotaWorkspaces());
        plan.setStatus(1);
        planRepository.save(plan);
        return toVo(plan);
    }

    @Transactional
    public PlanVO update(Long id, UpdatePlanRequest request) {
        Plan plan = requirePlan(id);
        plan.setName(request.name().trim());
        plan.setDescription(trimToNull(request.description()));
        plan.setPriceMonthly(request.priceMonthly());
        plan.setQuotaAiCalls(request.quotaAiCalls());
        plan.setQuotaTokens(request.quotaTokens());
        plan.setQuotaMembers(request.quotaMembers());
        plan.setQuotaWorkspaces(request.quotaWorkspaces());
        if (request.status() != 0 && request.status() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态值无效");
        }
        plan.setStatus(request.status());
        planRepository.update(plan);
        return toVo(plan);
    }

    public Plan requirePlan(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND, "套餐不存在"));
    }

    public PlanVO toVo(Plan plan) {
        return new PlanVO(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getPriceMonthly(),
                plan.getQuotaAiCalls(),
                plan.getQuotaTokens(),
                plan.getQuotaMembers(),
                plan.getQuotaWorkspaces(),
                plan.getStatus(),
                plan.getCreatedAt());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
