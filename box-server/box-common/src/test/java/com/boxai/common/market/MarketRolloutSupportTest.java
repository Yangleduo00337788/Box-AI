package com.boxai.common.market;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketRolloutSupportTest {

    @Test
    void rolloutPercentZeroHidesAll() {
        assertFalse(MarketRolloutSupport.isVisibleToTenant(1L, 10L, "GLOBAL", null, 0));
    }

    @Test
    void rolloutPercentFullShowsAll() {
        assertTrue(MarketRolloutSupport.isVisibleToTenant(1L, 10L, "GLOBAL", null, 100));
        assertTrue(MarketRolloutSupport.isVisibleToTenant(1L, 10L, "GLOBAL", null, null));
    }

    @Test
    void tenantVisibilityRequiresAllowList() {
        assertTrue(MarketRolloutSupport.isVisibleToTenant(2L, 5L, "TENANT", "[1,2,3]", 100));
        assertFalse(MarketRolloutSupport.isVisibleToTenant(9L, 5L, "TENANT", "[1,2,3]", 100));
    }

    @Test
    void bucketIsStable() {
        int first = MarketRolloutSupport.stableBucket(42L, 7L);
        int second = MarketRolloutSupport.stableBucket(42L, 7L);
        assertEquals(first, second);
        assertTrue(first >= 0 && first < 100);
    }

    @Test
    void serializeAndParseTenantIds() {
        String json = MarketRolloutSupport.serializeTenantIds(Set.of(1L, 2L, 3L));
        assertEquals(Set.of(1L, 2L, 3L), MarketRolloutSupport.parseTenantIds(json));
    }
}
