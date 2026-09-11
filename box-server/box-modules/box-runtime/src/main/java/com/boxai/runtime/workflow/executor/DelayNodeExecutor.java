package com.boxai.runtime.workflow.executor;

import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class DelayNodeExecutor implements NodeExecutor {

    private static final long MAX_DELAY_MS = 5000L;

    @Override
    public String nodeType() {
        return "Delay";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        long delayMs = config == null ? 0L : config.path("delayMs").asLong(0L);
        delayMs = Math.min(Math.max(delayMs, 0L), MAX_DELAY_MS);
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return NodeExecutionResult.failed("Delay 节点被中断");
            }
        }
        return NodeExecutionResult.ok();
    }
}
