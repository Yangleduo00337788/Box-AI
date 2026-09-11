package com.boxai.publish.controller;

import com.boxai.agent.api.AgentChatRequest;
import com.boxai.common.result.Result;
import com.boxai.publish.application.PublishedAgentApplicationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/published/agents")
public class PublishedAgentController {

    private final PublishedAgentApplicationService publishedAgentApplicationService;

    public PublishedAgentController(PublishedAgentApplicationService publishedAgentApplicationService) {
        this.publishedAgentApplicationService = publishedAgentApplicationService;
    }

    @PostMapping("/{agentId}/chat")
    public Object chat(@PathVariable Long agentId,
                       @Valid @RequestBody AgentChatRequest request,
                       HttpServletResponse response) {
        if (Boolean.TRUE.equals(request.stream())) {
            return publishedAgentApplicationService.streamChat(agentId, request, response);
        }
        return Result.success(publishedAgentApplicationService.chat(agentId, request));
    }
}
