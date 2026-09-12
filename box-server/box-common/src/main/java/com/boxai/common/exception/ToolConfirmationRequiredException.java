package com.boxai.common.exception;

import java.util.Map;

public class ToolConfirmationRequiredException extends BusinessException {

    private final String confirmationToken;
    private final String toolKey;
    private final String toolName;
    private final Map<String, Object> arguments;

    public ToolConfirmationRequiredException(String confirmationToken,
                                             String toolKey,
                                             String toolName,
                                             Map<String, Object> arguments) {
        super(ErrorCode.TOOL_CONFIRMATION_REQUIRED, "工具执行需要二次确认");
        this.confirmationToken = confirmationToken;
        this.toolKey = toolKey;
        this.toolName = toolName;
        this.arguments = arguments;
    }

    public String confirmationToken() {
        return confirmationToken;
    }

    public String toolKey() {
        return toolKey;
    }

    public String toolName() {
        return toolName;
    }

    public Map<String, Object> arguments() {
        return arguments;
    }
}
