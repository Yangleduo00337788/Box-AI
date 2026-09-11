package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class VariableNodeExecutor implements NodeExecutor {

    private final WorkflowTemplateRenderer templateRenderer;

    public VariableNodeExecutor(WorkflowTemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }

    @Override
    public String nodeType() {
        return "Variable";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        Map<String, Object> assigned = new LinkedHashMap<>();
        if (config != null && config.has("assignments") && config.get("assignments").isArray()) {
            for (JsonNode item : config.get("assignments")) {
                String name = item.path("name").asText(null);
                if (name == null || name.isBlank()) {
                    continue;
                }
                String rawValue = item.path("value").asText("");
                Object value = templateRenderer.render(rawValue, context.executionContext().variables());
                context.executionContext().setVariable(name, value);
                assigned.put(name, value);
            }
        }
        return NodeExecutionResult.ok(assigned);
    }
}
