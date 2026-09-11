package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
        Object actual = variable == null ? null : context.executionContext().getVariable(variable);
        boolean matched = evaluate(actual, operator, expected);
        String branch = matched ? "true" : "false";
        context.executionContext().setVariable("conditionResult", matched);
        return NodeExecutionResult.okWithBranch(branch);
    }

    private boolean evaluate(Object actual, String operator, String expected) {
        String actualText = actual == null ? null : String.valueOf(actual);
        return switch (operator == null ? "equals" : operator.toLowerCase()) {
            case "notequals", "not_equals", "!=" -> !Objects.equals(actualText, expected);
            case "contains" -> actualText != null && expected != null && actualText.contains(expected);
            case "empty" -> actualText == null || actualText.isBlank();
            case "notempty", "not_empty" -> actualText != null && !actualText.isBlank();
            default -> Objects.equals(actualText, expected);
        };
    }
}
