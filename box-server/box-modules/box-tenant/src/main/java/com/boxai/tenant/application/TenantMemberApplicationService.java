package com.boxai.tenant.application;

import com.boxai.common.constant.RoleCodes;
import com.boxai.common.constant.TenantTypes;
import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantMember;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.tenant.api.AddTenantMemberRequest;
import com.boxai.tenant.api.TenantMemberVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class TenantMemberApplicationService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final QuotaApplicationService quotaApplicationService;

    public TenantMemberApplicationService(TenantRepository tenantRepository,
                                          UserRepository userRepository,
                                          QuotaApplicationService quotaApplicationService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.quotaApplicationService = quotaApplicationService;
    }

    public List<TenantMemberVO> listMembers(Long tenantId) {
        requireTenant(tenantId);
        return tenantRepository.listMembersByTenantId(tenantId).stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public TenantMemberVO addMember(Long tenantId, AddTenantMemberRequest request) {
        Tenant tenant = requireTenant(tenantId);
        if (TenantTypes.PERSONAL.equals(tenant.getTenantType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "个人租户不支持添加成员");
        }
        User user = userRepository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在，请先注册"));
        if (UserTypes.PLATFORM_ADMIN.equals(user.getUserType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能添加平台管理员为租户成员");
        }
        if (tenantRepository.findMember(tenantId, user.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该用户已是租户成员");
        }
        quotaApplicationService.assertMemberQuotaAvailable(tenantId);
        TenantMember member = new TenantMember();
        member.setTenantId(tenantId);
        member.setUserId(user.getId());
        member.setRoleCode(normalizeRoleCode(request.roleCode()));
        member.setStatus(1);
        tenantRepository.addMember(member);
        return toVo(member);
    }

    @Transactional
    public TenantMemberVO updateMemberStatus(Long tenantId, Long userId, Integer status) {
        requireTenant(tenantId);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态值无效");
        }
        TenantMember member = tenantRepository.findMember(tenantId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "租户成员不存在"));
        member.setStatus(status);
        tenantRepository.updateMember(member);
        return toVo(member);
    }

    public List<TenantMemberVO> listMyTenantMembers(Long operatorUserId) {
        TenantMember operator = requirePrimaryMember(operatorUserId);
        requireTenantAdmin(operator);
        return listMembers(operator.getTenantId());
    }

    @Transactional
    public TenantMemberVO addMyTenantMember(Long operatorUserId, AddTenantMemberRequest request) {
        TenantMember operator = requirePrimaryMember(operatorUserId);
        requireTenantAdmin(operator);
        return addMember(operator.getTenantId(), request);
    }

    @Transactional
    public TenantMemberVO updateMyTenantMemberStatus(Long operatorUserId, Long userId, Integer status) {
        TenantMember operator = requirePrimaryMember(operatorUserId);
        requireTenantAdmin(operator);
        if (operator.getUserId().equals(userId) && status != null && status == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能停用当前登录账号");
        }
        return updateMemberStatus(operator.getTenantId(), userId, status);
    }

    private Tenant requireTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "租户不存在"));
    }

    private TenantMember requirePrimaryMember(Long userId) {
        return tenantRepository.findPrimaryByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "未找到租户"));
    }

    private void requireTenantAdmin(TenantMember member) {
        if (!RoleCodes.TENANT_ADMIN.equals(member.getRoleCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要租户管理员权限");
        }
        if (member.getStatus() == null || member.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "租户成员已停用");
        }
    }

    private String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return RoleCodes.MEMBER;
        }
        if (RoleCodes.TENANT_ADMIN.equals(roleCode) || RoleCodes.MEMBER.equals(roleCode)) {
            return roleCode;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "角色无效");
    }

    private TenantMemberVO toVo(TenantMember member) {
        User user = userRepository.findById(member.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        return new TenantMemberVO(
                member.getId(),
                member.getUserId(),
                user.getEmail(),
                user.getNickname(),
                member.getRoleCode(),
                member.getStatus(),
                member.getJoinedAt());
    }
}
