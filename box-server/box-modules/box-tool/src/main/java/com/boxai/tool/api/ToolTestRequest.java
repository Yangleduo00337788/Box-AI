package com.boxai.tool.api;

import java.util.Map;

public record ToolTestRequest(
        String sql,
        Map<String, Object> arguments
) {
}
