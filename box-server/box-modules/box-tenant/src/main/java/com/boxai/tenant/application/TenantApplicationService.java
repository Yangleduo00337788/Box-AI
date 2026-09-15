package com.boxai.tenant.application;

import com.boxai.common.constant.RoleCodes;
import com.boxai.common.constant.TenantTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plan.Plan;
import com.boxai.domain.plan.PlanRepository;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantMember;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.tenant.TenantAccessGuard;
import com.boxai.tenant.api.AdminWorkspaceVO;
import com.boxai.tenant.api.CreateTenantRequest;
import com.boxai.tenant.api.TenantVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TenantApplicationService {

    private final TenantRepository tenantRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final QuotaApplicationService quotaApplicationService;
    private final PlanApplicationService planApplicationService;
    private final WorkspaceRepository workspaceRepository;
    private final PlanRepository planRepository;

    public TenantApplicationService(TenantRepository tenantRepository,
                                    TenantAccessGuard tenantAccessGuard,
                                    QuotaApplicationService quotaApplicationService,
                                    PlanApplicationService planApplicationService,
                                    WorkspaceRepository workspaceRepository,
                                    PlanRepository planRepository) {
        this.tenantRepository = tenantRepository;
        this.tenantAccessGuard = tenantAccessGuard;
        this.quotaApplicationService = quotaApplicationService;
        this.planApplicationService = planApplicationService;
        this.workspaceRepository = workspaceRepository;
        this.planRepository = planRepository;
    }

    @Transactional
    public Tenant createForRegistration(User user, String accountType, String companyName, String contactEmail) {
        String normalizedType = normalizeAccountType(accountType);
        Tenant tenant = new Tenant();
        if (TenantTypes.ENTERPRISE.equals(normalizedType)) {
            tenant.setName(companyName.trim());
            tenant.setSlug(uniqueSlug(companyName));
            tenant.setContactEmail(contactEmail == null || contactEmail.isBlank() ? user.getEmail() : contactEmail.trim());
            tenant.setTenantType(TenantTypes.ENTERPRISE);
        } else {
            tenant.setName(user.getNickname() + " 的个人空间");
            tenant.setSlug(uniqueSlug(user.getNickname() + "-personal"));
            tenant.setContactEmail(user.getEmail());
            tenant.setTenantType(TenantTypes.PERSONAL);
        }
        tenant.setOwnerId(user.getId());
        tenant.setStatus(1);
        quotaApplicationService.assignDefaultPlan(tenant);
        tenantRepository.save(tenant);

        TenantMember member = new TenantMember();
        member.setTenantId(tenant.getId());
        member.setUserId(user.getId());
        member.setRoleCode(RoleCodes.TENANT_ADMIN);
        member.setStatus(1);
        tenantRepository.addMember(member);
        return tenant;
    }

    public List<TenantVO> listAll() {
        return tenantRepository.listAll().stream().map(this::toVo).toList();
    }

    public List<AdminWorkspaceVO> listWorkspaces(Long tenantId) {
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "租户不存在"));
        return workspaceRepository.listByTenantId(tenantId).stream()
                .map(item -> new AdminWorkspaceVO(
                        item.getId(),
                        item.getName(),
                        item.getSlug(),
                        item.getDescription(),
                        item.getAvatarUrl(),
                        item.getStatus(),
                        item.getOwnerId(),
                        item.getCreatedAt()))
                .toList();
    }

    @Transactional
    public TenantVO create(CreateTenantRequest request) {
        String slug = request.slug();
        if (slug == null || slug.isBlank()) {
            slug = uniqueSlug(request.name());
        } else {
            slug = normalizeSlug(slug);
        }
        if (tenantRepository.findBySlug(slug).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "租户标识已存在");
        }
        Tenant tenant = new Tenant();
        tenant.setName(request.name().trim());
        tenant.setSlug(slug);
        tenant.setTenantType(normalizeAccountType(request.tenantType()));
        tenant.setContactEmail(request.contactEmail());
        tenant.setStatus(1);
        quotaApplicationService.assignDefaultPlan(tenant);
        tenantRepository.save(tenant);
        return toVo(tenant);
    }

    @Transactional
    public TenantVO assignPlan(Long id, Long planId) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "租户不存在"));
        planApplicationService.requirePlan(planId);
        tenant.setPlanId(planId);
        tenantRepository.update(tenant);
        return toVo(tenant);
    }

    @Transactional
    public TenantVO updateStatus(Long id, Integer status) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "租户不存在"));
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态值无效");
        }
        tenant.setStatus(status);
        tenantRepository.update(tenant);
        return toVo(tenant);
    }

    public TenantVO requireById(Long id) {
        return tenantRepository.findById(id)
                .map(this::toVo)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "租户不存在"));
    }

    public TenantVO findPrimaryByUserId(Long userId) {
        return tenantRepository.findPrimaryByUserId(userId)
                .flatMap(member -> tenantRepository.findById(member.getTenantId()).map(this::toVo))
                .orElse(null);
    }

    public void ensurePrimaryTenantActive(Long userId) {
        tenantAccessGuard.ensurePrimaryTenantActive(userId);
    }

    @Transactional
    public TenantVO upgradeToEnterprise(Long userId, String companyName) {
        TenantMember member = tenantRepository.findPrimaryByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "未找到租户"));
        Tenant tenant = tenantRepository.findById(member.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "租户不存在"));
        if (!TenantTypes.PERSONAL.equals(tenant.getTenantType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前已是企业版");
        }
        if (companyName == null || companyName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写企业名称");
        }
        tenant.setTenantType(TenantTypes.ENTERPRISE);
        tenant.setName(companyName.trim());
        tenant.setSlug(uniqueSlug(companyName));
        Plan plan = planRepository.findByCode("enterprise_starter")
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND, "企业套餐不存在"));
        tenant.setPlanId(plan.getId());
        tenantRepository.update(tenant);
        return toVo(tenant);
    }

    private TenantVO toVo(Tenant tenant) {
        String planName = null;
        if (tenant.getPlanId() != null) {
            planName = planApplicationService.requirePlan(tenant.getPlanId()).getName();
        }
        return new TenantVO(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getTenantType(),
                tenant.getPlanId(),
                planName,
                tenant.getContactEmail(),
                tenant.getStatus(),
                tenant.getOwnerId(),
                tenant.getCreatedAt());
    }

    private String normalizeAccountType(String accountType) {
        if (TenantTypes.PERSONAL.equalsIgnoreCase(accountType)) {
            return TenantTypes.PERSONAL;
        }
        if (TenantTypes.ENTERPRISE.equalsIgnoreCase(accountType)) {
            return TenantTypes.ENTERPRISE;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "账号类型无效");
    }

    private String uniqueSlug(String name) {
        String base = normalizeSlug(name);
        if (base.isBlank()) {
            base = "tenant";
        }
        String slug = base;
        int i = 1;
        while (tenantRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + i++;
            if (i > 20) {
                slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }
        return slug;
    }

    private String normalizeSlug(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
