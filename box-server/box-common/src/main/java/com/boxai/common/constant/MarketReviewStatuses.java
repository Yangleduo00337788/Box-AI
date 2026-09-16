package com.boxai.common.constant;

import java.util.Set;

public final class MarketReviewStatuses {

    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    public static final Set<String> ALL = Set.of(PENDING_REVIEW, APPROVED, REJECTED);

    private MarketReviewStatuses() {
    }

    public static boolean isApproved(String reviewStatus) {
        return APPROVED.equals(reviewStatus);
    }

    public static boolean visibleToConsumers(String listingStatus, String reviewStatus) {
        return "LISTED".equals(listingStatus) && isApproved(reviewStatus);
    }
}
