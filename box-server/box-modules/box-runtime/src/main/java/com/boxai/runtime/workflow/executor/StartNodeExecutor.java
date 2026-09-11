package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import org.springframework.stereotype.Component;

@Component
public class StartNodeExecutor implements NodeExecutor {

    @Override
    public String nodeType() {
        return "Start";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        return NodeExecutionResult.ok();
    }
}
