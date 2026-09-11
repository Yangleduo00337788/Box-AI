package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;

public interface NodeExecutor {

    String nodeType();

    NodeExecutionResult execute(NodeExecutionContext context);
}
