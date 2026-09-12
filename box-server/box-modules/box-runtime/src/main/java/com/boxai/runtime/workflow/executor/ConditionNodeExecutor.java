package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.util.ConditionEvaluator;
import com.boxai.runtime.workflow.util.WorkflowVariableResolver;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class ConditionNodeExecutor implements NodeExecutor {

    @Override
    public String nodeType() {
        return "Condition";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        String variable = config == null ? null : config.path("variable").asText(null);
        String operator = config == null ? "equals" : config.path("operator").asText("equals");
        String expected = config == null ? null : config.path("value").asText(null);
        Object actual = variable == null ? null : WorkflowVariableResolver.resolve(context.executionContext(), variable);
        boolean matched = ConditionEvaluator.evaluate(actual, operator, expected);
        String branch = matched ? "true" : "false";
        context.executionContext().setVariable("conditionResult", matched);
        return NodeExecutionResult.okWithBranch(branch);
    }
}
