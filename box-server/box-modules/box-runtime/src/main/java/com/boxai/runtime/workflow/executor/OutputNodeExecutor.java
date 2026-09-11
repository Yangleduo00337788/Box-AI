package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OutputNodeExecutor implements NodeExecutor {

    @Override
    public String nodeType() {
        return "Output";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        Map<String, Object> nodeOutput = new LinkedHashMap<>();
        if (config != null && config.has("outputKey") && config.has("variable")) {
            String outputKey = config.get("outputKey").asText();
            String variable = config.get("variable").asText();
            Object value = context.executionContext().getVariable(variable);
            context.executionContext().setOutput(outputKey, value);
            nodeOutput.put(outputKey, value);
        } else if (config != null && config.has("outputs") && config.get("outputs").isObject()) {
            config.get("outputs").fields().forEachRemaining(entry -> {
                Object value = context.executionContext().getVariable(entry.getValue().asText());
                context.executionContext().setOutput(entry.getKey(), value);
                nodeOutput.put(entry.getKey(), value);
            });
        } else {
            context.executionContext().outputs().putAll(context.executionContext().variables());
            nodeOutput.putAll(context.executionContext().variables());
        }
        return NodeExecutionResult.terminal(nodeOutput);
    }
}
