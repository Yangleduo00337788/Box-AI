package com.boxai.agent.chat;

import java.util.Map;

public interface AgentWorkflowInvoker {

    String invokeDraft(Long workflowId, Map<String, Object> inputs);
}
