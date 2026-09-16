package com.boxai.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketReviewStatusesTest {

    @Test
    void consumersOnlySeeListedAndApproved() {
        assertTrue(MarketReviewStatuses.visibleToConsumers("LISTED", MarketReviewStatuses.APPROVED));
        assertFalse(MarketReviewStatuses.visibleToConsumers("LISTED", MarketReviewStatuses.PENDING_REVIEW));
        assertFalse(MarketReviewStatuses.visibleToConsumers("LISTED", MarketReviewStatuses.REJECTED));
        assertFalse(MarketReviewStatuses.visibleToConsumers("DRAFT", MarketReviewStatuses.APPROVED));
        assertFalse(MarketReviewStatuses.visibleToConsumers("ARCHIVED", MarketReviewStatuses.APPROVED));
    }
}
