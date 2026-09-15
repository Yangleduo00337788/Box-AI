package com.boxai.tenant.api;

import jakarta.validation.constraints.NotNull;

public record SubscribePlanRequest(@NotNull Long planId) {
}
