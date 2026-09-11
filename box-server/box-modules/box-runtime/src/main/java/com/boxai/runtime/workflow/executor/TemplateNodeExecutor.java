package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TemplateNodeExecutor implements NodeExecutor {

    private final WorkflowTemplateRenderer templateRenderer;

    public TemplateNodeExecutor(WorkflowTemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }

    @Override
    public String nodeType() {
        return "Template";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        String template = config == null ? "" : config.path("template").asText("");
        String outputVariable = config == null ? "templateResult" : config.path("outputVariable").asText("templateResult");
        String rendered = templateRenderer.render(template, context.executionContext().variables());
        context.executionContext().setVariable(outputVariable, rendered);
        return NodeExecutionResult.ok(Map.of(outputVariable, rendered));
    }
}
