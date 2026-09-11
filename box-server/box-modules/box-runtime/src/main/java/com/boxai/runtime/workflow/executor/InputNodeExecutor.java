package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class InputNodeExecutor implements NodeExecutor {

    @Override
    public String nodeType() {
        return "Input";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        if (config == null || !config.isObject()) {
            return NodeExecutionResult.ok();
        }
        config.fields().forEachRemaining(entry -> {
            Object value = context.executionContext().getVariable(entry.getKey());
            if (value != null) {
                context.executionContext().setVariable(entry.getKey(), value);
            }
        });
        if (config.has("inputKey") && config.has("variable")) {
            String inputKey = config.get("inputKey").asText();
            String variable = config.get("variable").asText();
            Object value = context.executionContext().getVariable(inputKey);
            if (value != null) {
                context.executionContext().setVariable(variable, value);
            }
        }
        return NodeExecutionResult.ok();
    }
}
