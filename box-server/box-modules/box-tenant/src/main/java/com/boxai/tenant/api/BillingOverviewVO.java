package com.boxai.tenant.api;

import java.math.BigDecimal;

public record BillingOverviewVO(
        String period,
        String planName,
        BigDecimal planPriceMonthly,
        Integer usedAiCalls,
        Long usedTokens,
        Integer quotaAiCalls,
        Long quotaTokens,
        Integer overageAiCalls,
        Long overageTokens,
        String overagePolicy,
        BigDecimal estimatedAmount,
        String currency,
        boolean paymentEnabled
) {
}
