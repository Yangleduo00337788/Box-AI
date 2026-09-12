package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.util.ConditionEvaluator;
import com.boxai.runtime.workflow.util.WorkflowVariableResolver;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class SwitchNodeExecutor implements NodeExecutor {

    @Override
    public String nodeType() {
        return "Switch";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        if (config == null || !config.isObject()) {
            return NodeExecutionResult.failed("Switch 节点缺少 config");
        }
        String variable = config.path("variable").asText(null);
        if (variable == null || variable.isBlank()) {
            return NodeExecutionResult.failed("Switch 节点缺少 variable");
        }
        String operator = config.path("operator").asText("equals");
        String defaultCase = config.path("defaultCase").asText("default");
        Object actual = WorkflowVariableResolver.resolve(context.executionContext(), variable);
        JsonNode cases = config.get("cases");
        if (cases != null && cases.isArray()) {
            for (JsonNode caseNode : cases) {
                if (!caseNode.isObject()) {
                    continue;
                }
                String caseId = caseNode.path("id").asText(null);
                String expected = caseNode.path("value").asText(null);
                if (caseId == null || caseId.isBlank()) {
                    continue;
                }
                if (ConditionEvaluator.evaluate(actual, operator, expected)) {
                    context.executionContext().setVariable("switchResult", caseId);
                    return NodeExecutionResult.okWithBranch(caseId);
                }
            }
        }
        context.executionContext().setVariable("switchResult", defaultCase);
        return NodeExecutionResult.okWithBranch(defaultCase);
    }
}
