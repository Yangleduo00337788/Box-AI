package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.tool.application.InlineScriptExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CodeNodeExecutor implements NodeExecutor {

    private final InlineScriptExecutor inlineScriptExecutor;

    public CodeNodeExecutor(InlineScriptExecutor inlineScriptExecutor) {
        this.inlineScriptExecutor = inlineScriptExecutor;
    }

    @Override
    public String nodeType() {
        return "Code";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        if (config == null || !config.has("code")) {
            return NodeExecutionResult.failed("Code 节点缺少 code 配置");
        }
        String code = config.path("code").asText("");
        String functionName = config.path("functionName").asText("execute");
        String outputVariable = config.path("outputVariable").asText("codeResult");
        int timeoutMs = config.path("timeoutMs").asInt(5000);
        Map<String, Object> args = new HashMap<>(context.executionContext().variables());
        String result = inlineScriptExecutor.execute(code, functionName, args, timeoutMs);
        context.executionContext().setVariable(outputVariable, result);
        return NodeExecutionResult.ok(Map.of(outputVariable, result));
    }
}
