package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.TenantUsageDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TenantUsageMapper extends BaseMapper<TenantUsageDO> {

    @Update("""
            UPDATE tenant_usage
            SET ai_calls = ai_calls + #{aiCalls},
                tokens = tokens + #{tokens},
                overage_ai_calls = IFNULL(overage_ai_calls, 0) + #{overageAiCalls},
                overage_tokens = IFNULL(overage_tokens, 0) + #{overageTokens},
                updated_at = NOW()
            WHERE tenant_id = #{tenantId} AND period = #{period}
            """)
    int incrementUsage(@Param("tenantId") Long tenantId,
                       @Param("period") String period,
                       @Param("aiCalls") int aiCalls,
                       @Param("tokens") long tokens,
                       @Param("overageAiCalls") int overageAiCalls,
                       @Param("overageTokens") long overageTokens);
}
