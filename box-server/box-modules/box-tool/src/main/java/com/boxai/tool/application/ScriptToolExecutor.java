package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.tool.ToolFunctionConfig;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ScriptToolExecutor {

    private final InlineScriptExecutor inlineScriptExecutor;

    public ScriptToolExecutor(InlineScriptExecutor inlineScriptExecutor) {
        this.inlineScriptExecutor = inlineScriptExecutor;
    }

    public String execute(ToolFunctionConfig config, Map<String, Object> arguments) {
        String runtime = config.getRuntime() == null ? "JAVA_SCRIPT" : config.getRuntime().trim().toUpperCase();
        if (!"JAVA_SCRIPT".equals(runtime)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前仅支持 JAVA_SCRIPT 运行时");
        }
        int timeoutMs = config.getTimeoutMs() == null ? 5000 : config.getTimeoutMs();
        return inlineScriptExecutor.execute(
                config.getFunctionCode(),
                config.getFunctionName(),
                arguments,
                timeoutMs);
    }
}
