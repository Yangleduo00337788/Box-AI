package com.boxai.tenant.application;

import com.boxai.common.constant.OveragePolicies;
import com.boxai.common.constant.PlanAudiences;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plan.Plan;
import com.boxai.domain.plan.PlanRepository;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.tenant.api.CreatePlanRequest;
import com.boxai.tenant.api.UpdatePlanRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanApplicationServiceTest {

    @Mock
    private PlanRepository planRepository;
    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private PlanApplicationService service;

    @Test
    void createRejectsDuplicateCode() {
        when(planRepository.findByCode("pro")).thenReturn(Optional.of(new Plan()));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(createRequest("PRO", "Pro")));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(planRepository, never()).save(any());
    }

    @Test
    void createNormalizesCodeAudienceAndOverage() {
        when(planRepository.findByCode("pro")).thenReturn(Optional.empty());

        var vo = service.create(createRequest(" PRO ", "专业版"));

        ArgumentCaptor<Plan> captor = ArgumentCaptor.forClass(Plan.class);
        verify(planRepository).save(captor.capture());
        assertEquals("pro", captor.getValue().getCode());
        assertEquals(PlanAudiences.PERSONAL, captor.getValue().getAudience());
        assertEquals(OveragePolicies.REJECT, captor.getValue().getOveragePolicy());
        assertEquals(1, captor.getValue().getStatus());
        assertEquals("pro", vo.code());
    }

    @Test
    void createRejectsInvalidOveragePolicy() {
        when(planRepository.findByCode("pro")).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(
                new CreatePlanRequest("pro", "Pro", null, BigDecimal.ONE, 10, 100L, 3, 2, 1, "PERSONAL", "UNLIMITED")));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void updateRejectsInvalidStatus() {
        Plan plan = new Plan();
        plan.setId(4L);
        when(planRepository.findById(4L)).thenReturn(Optional.of(plan));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(4L, new UpdatePlanRequest(
                "Pro", null, BigDecimal.ONE, 10, 100L, 3, 2, 1, "PERSONAL", "REJECT", 9)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(planRepository, never()).update(any());
    }

    @Test
    void deleteRejectsPlanStillInUse() {
        when(planRepository.findById(4L)).thenReturn(Optional.of(new Plan()));
        when(tenantRepository.countByPlanId(4L)).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(4L));
        assertEquals(ErrorCode.CONFLICT, ex.getCode());
        verify(planRepository, never()).delete(anyLong());
    }

    @Test
    void listInfersTeamAudienceFromChineseName() {
        Plan plan = new Plan();
        plan.setId(5L);
        plan.setCode("biz");
        plan.setName("企业套餐");
        plan.setQuotaKnowledgeBases(null);
        plan.setByokEnabled(null);
        plan.setOveragePolicy(null);
        when(planRepository.listAll()).thenReturn(List.of(plan));

        var vo = service.listAll().get(0);
        assertEquals(PlanAudiences.TEAM, vo.audience());
        assertEquals(OveragePolicies.REJECT, vo.overagePolicy());
        assertEquals(0, vo.quotaKnowledgeBases());
    }

    private static CreatePlanRequest createRequest(String code, String name) {
        return new CreatePlanRequest(code, name, "  ", BigDecimal.TEN, 100, 1000L, 5, 3, 2, null, null);
    }
}
