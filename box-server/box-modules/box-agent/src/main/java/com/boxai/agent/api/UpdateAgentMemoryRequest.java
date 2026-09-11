package com.boxai.agent.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateAgentMemoryRequest(
        Boolean memoryEnabled,
        @Min(0) @Max(100) Integer memoryWindowSize
) {
}
