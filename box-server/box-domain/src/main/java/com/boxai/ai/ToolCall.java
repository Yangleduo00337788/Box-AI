package com.boxai.ai;

import java.util.Map;

public record ToolCall(String toolKey, Map<String, Object> arguments) {
}
