package com.boxai.security.tenant;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantMember;
import com.boxai.domain.tenant.TenantRepository;
import org.springframework.stereotype.Component;

@Component
public class TenantAccessGuard {

    private final TenantRepository tenantRepository;

    public TenantAccessGuard(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public void ensurePrimaryTenantActive(Long userId) {
        TenantMember member = tenantRepository.findPrimaryByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "未找到租户"));
        Tenant tenant = tenantRepository.findById(member.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "未找到租户"));
        if (tenant.getStatus() == null || tenant.getStatus() != 1) {
            throw new BusinessException(ErrorCode.TENANT_DISABLED, "租户已停用，请联系平台管理员");
        }
    }
}
