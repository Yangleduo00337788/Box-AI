package com.boxai.tenant.application;

import com.boxai.common.constant.RoleCodes;
import com.boxai.common.constant.TenantTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.EnterpriseAccounts;
import com.boxai.domain.plan.Plan;
import com.boxai.domain.plan.PlanRepository;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantMember;
import com.boxai.domain.tenant.TenantOAuthOrg;
import com.boxai.domain.tenant.TenantOAuthOrgRepository;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.tenant.TenantAccessGuard;
import com.boxai.tenant.api.AdminWorkspaceVO;
import com.boxai.tenant.api.CreateTenantRequest;
import com.boxai.tenant.api.EnterpriseOrgAccessVO;
import com.boxai.tenant.api.TenantVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class TenantApplicationService {

    private final TenantRepository tenantRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final QuotaApplicationService quotaApplicationService;
    private final PlanApplicationService planApplicationService;
    private final WorkspaceRepository workspaceRepository;
    private final PlanRepository planRepository;
    private final TenantOAuthOrgRepository tenantOAuthOrgRepository;

    public TenantApplicationService(TenantRepository tenantRepository,
                                    TenantAccessGuard tenantAccessGuard,
                                    QuotaApplicationService quotaApplicationService,
                                    PlanApplicationService planApplicationService,
                                    WorkspaceRepository workspaceRepository,
                                    PlanRepository planRepository,
                                    TenantOAuthOrgRepository tenantOAuthOrgRepository) {
        this.tenantRepository = tenantRepository;
        this.tenantAccessGuard = tenantAccessGuard;
        this.quotaApplicationService = quotaApplicationService;
        this.planApplicationService = planApplicationService;
        this.workspaceRepository = workspaceRepository;
        this.planRepository = planRepository;
        this.tenantOAuthOrgRepository = tenantOAuthOrgRepository;
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
            tenant.setInviteCode(newUniqueInviteCode());
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
        member.setLoginName(defaultLoginName(user));
        member.setStatus(1);
        tenantRepository.addMember(member);
        return tenant;
    }

    public Optional<Tenant> findByOAuthOrg(String provider, String orgId) {
        if (provider == null || orgId == null || orgId.isBlank()) {
            return Optional.empty();
        }
        return tenantOAuthOrgRepository.findByProviderAndOrgId(provider, orgId)
                .flatMap(binding -> tenantRepository.findById(binding.getTenantId()));
    }

    @Transactional
    public void bindOAuthOrg(Long tenantId, String provider, String orgId) {
        if (tenantId == null || provider == null || orgId == null || orgId.isBlank()) {
            return;
        }
        if (tenantOAuthOrgRepository.findByProviderAndOrgId(provider, orgId).isPresent()) {
            return;
        }
        TenantOAuthOrg binding = new TenantOAuthOrg();
        binding.setTenantId(tenantId);
        binding.setProvider(provider);
        binding.setOrgId(orgId);
        tenantOAuthOrgRepository.save(binding);
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
        if (tenant.getInviteCode() == null || tenant.getInviteCode().isBlank()) {
            tenant.setInviteCode(newUniqueInviteCode());
        }
        tenantRepository.update(tenant);
        return toVo(tenant);
    }

    public Optional<Tenant> findEnterpriseByOrgId(String orgId) {
        String slug = EnterpriseAccounts.normalizeOrgId(orgId);
        return tenantRepository.findBySlug(slug)
                .filter(item -> TenantTypes.ENTERPRISE.equals(item.getTenantType())
                        && item.getStatus() != null
                        && item.getStatus() == 1);
    }

    @Transactional
    public EnterpriseOrgAccessVO getOrgAccess(Long operatorUserId) {
        Tenant tenant = requireAdminEnterprise(operatorUserId);
        if (tenant.getInviteCode() == null || tenant.getInviteCode().isBlank()) {
            tenant.setInviteCode(newUniqueInviteCode());
            tenantRepository.update(tenant);
        }
        return new EnterpriseOrgAccessVO(tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.getInviteCode());
    }

    @Transactional
    public EnterpriseOrgAccessVO updateOrgId(Long operatorUserId, String orgId) {
        Tenant tenant = requireAdminEnterprise(operatorUserId);
        String slug = normalizeSlug(orgId);
        if (slug.length() < 2 || slug.length() > 64) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "企业标识为 2-64 位字母、数字或短横线");
        }
        if (!slug.equals(tenant.getSlug()) && tenantRepository.findBySlug(slug).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "企业标识已被占用");
        }
        tenant.setSlug(slug);
        tenantRepository.update(tenant);
        return new EnterpriseOrgAccessVO(tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.getInviteCode());
    }

    @Transactional
    public EnterpriseOrgAccessVO rotateInviteCode(Long operatorUserId) {
        Tenant tenant = requireAdminEnterprise(operatorUserId);
        tenant.setInviteCode(newUniqueInviteCode());
        tenantRepository.update(tenant);
        return new EnterpriseOrgAccessVO(tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.getInviteCode());
    }

    public Tenant requireMatchingInvite(String orgId, String inviteCode) {
        Tenant tenant = findEnterpriseByOrgId(orgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "企业标识或邀请码无效"));
        String code = inviteCode == null ? "" : inviteCode.trim().toUpperCase(Locale.ROOT);
        if (tenant.getInviteCode() == null || !tenant.getInviteCode().equalsIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "企业标识或邀请码无效");
        }
        return tenant;
    }

    private Tenant requireAdminEnterprise(Long operatorUserId) {
        TenantMember operator = tenantRepository.findPrimaryByUserId(operatorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "未找到租户"));
        if (!RoleCodes.TENANT_ADMIN.equals(operator.getRoleCode())
                || operator.getStatus() == null
                || operator.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要租户管理员权限");
        }
        Tenant tenant = tenantRepository.findById(operator.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "租户不存在"));
        if (!TenantTypes.ENTERPRISE.equals(tenant.getTenantType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅企业租户支持该操作");
        }
        return tenant;
    }

    private String newUniqueInviteCode() {
        for (int i = 0; i < 8; i++) {
            String code = EnterpriseAccounts.randomInviteCode();
            if (tenantRepository.findByInviteCode(code).isEmpty()) {
                return code;
            }
        }
        return EnterpriseAccounts.randomInviteCode() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
    }

    private static String defaultLoginName(User user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail().trim().toLowerCase(Locale.ROOT);
        }
        return user.getUsername() == null ? null : user.getUsername().trim().toLowerCase(Locale.ROOT);
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
