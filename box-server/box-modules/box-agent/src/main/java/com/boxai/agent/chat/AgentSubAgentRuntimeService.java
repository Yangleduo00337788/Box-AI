package com.boxai.agent.chat;

import com.boxai.ai.ChatTurn;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AgentSubAgentRuntimeService {

    private final AgentRepository agentRepository;
    private final AgentChatPreparer agentChatPreparer;
    private final AgentChatExecutor agentChatExecutor;

    public AgentSubAgentRuntimeService(AgentRepository agentRepository,
                                       AgentChatPreparer agentChatPreparer,
                                       @Lazy AgentChatExecutor agentChatExecutor) {
        this.agentRepository = agentRepository;
        this.agentChatPreparer = agentChatPreparer;
        this.agentChatExecutor = agentChatExecutor;
    }

    public String delegate(Long subAgentId, Map<String, Object> arguments) {
        Agent subAgent = agentRepository.findById(subAgentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND, "子智能体不存在"));
        String message = extractMessage(arguments);
        PreparedAgentChat prepared = agentChatPreparer.prepare(subAgent.getId(), List.<ChatTurn>of(), message);
        return agentChatExecutor.chat(prepared);
    }

    public static String subAgentToolKey(String agentKey) {
        return "agent_" + sanitize(agentKey);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase().replaceAll("[^a-z0-9_]+", "_");
    }

    private String extractMessage(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "子智能体调用缺少 message 参数");
        }
        Object message = arguments.containsKey("message") ? arguments.get("message") : arguments.get("query");
        if (message == null || String.valueOf(message).isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "子智能体调用缺少 message 参数");
        }
        return String.valueOf(message);
    }
}
