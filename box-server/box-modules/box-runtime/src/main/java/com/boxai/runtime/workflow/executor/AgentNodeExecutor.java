package com.boxai.runtime.workflow.executor;

import com.boxai.agent.chat.AgentChatExecutor;
import com.boxai.agent.chat.AgentChatPreparer;
import com.boxai.agent.chat.PreparedAgentChat;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.runtime.workflow.core.NodeExecutionContext;
import com.boxai.runtime.workflow.core.NodeExecutionResult;
import com.boxai.runtime.workflow.engine.WorkflowTemplateRenderer;
import com.boxai.security.context.WorkspaceContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AgentNodeExecutor implements NodeExecutor {

    private final AgentRepository agentRepository;
    private final AgentChatPreparer agentChatPreparer;
    private final AgentChatExecutor agentChatExecutor;
    private final WorkflowTemplateRenderer templateRenderer;

    public AgentNodeExecutor(AgentRepository agentRepository,
                             AgentChatPreparer agentChatPreparer,
                             AgentChatExecutor agentChatExecutor,
                             WorkflowTemplateRenderer templateRenderer) {
        this.agentRepository = agentRepository;
        this.agentChatPreparer = agentChatPreparer;
        this.agentChatExecutor = agentChatExecutor;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public String nodeType() {
        return "Agent";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode config = context.node().config();
        if (config == null || !config.has("agentId")) {
            return NodeExecutionResult.failed("Agent 节点缺少 agentId");
        }
        long agentId = config.get("agentId").asLong();
        String message = templateRenderer.render(
                config.path("message").asText("{{input.message}}"),
                context.executionContext().variables());
        if (message.isBlank()) {
            return NodeExecutionResult.failed("Agent 节点消息不能为空");
        }

        Agent agent = agentRepository.findById(agentId).orElse(null);
        if (agent == null) {
            return NodeExecutionResult.failed("智能体不存在");
        }
        if (!WorkspaceContext.require().workspaceId().equals(agent.getWorkspaceId())) {
            return NodeExecutionResult.failed("无权访问该智能体");
        }

        try {
            PreparedAgentChat prepared = context.executionContext().debugMode()
                    ? agentChatPreparer.prepare(agentId, List.of(), message)
                    : agentChatPreparer.preparePublished(agentId, List.of(), message);
            String content = agentChatExecutor.chat(prepared);
            String outputVariable = config.path("outputVariable").asText("agentResult");
            Map<String, Object> output = Map.of(
                    "agentId", agentId,
                    "content", content);
            context.executionContext().setVariable(outputVariable, output);
            return NodeExecutionResult.ok(Map.of(outputVariable, output));
        } catch (Exception ex) {
            return NodeExecutionResult.failed("Agent 节点执行失败: " + ex.getMessage());
        }
    }
}
