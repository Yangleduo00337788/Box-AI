package com.boxai.conversation.controller;

import com.boxai.common.result.Result;
import com.boxai.conversation.api.ConversationVO;
import com.boxai.conversation.api.CreateConversationRequest;
import com.boxai.conversation.api.MessageVO;
import com.boxai.conversation.api.RegenerateMessageRequest;
import com.boxai.conversation.api.SendMessageRequest;
import com.boxai.conversation.api.SendMessageVO;
import com.boxai.conversation.application.ConversationApplicationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationApplicationService conversationApplicationService;

    public ConversationController(ConversationApplicationService conversationApplicationService) {
        this.conversationApplicationService = conversationApplicationService;
    }

    @PostMapping
    public Result<ConversationVO> create(@Valid @RequestBody CreateConversationRequest request) {
        return Result.success(conversationApplicationService.create(request));
    }

    @GetMapping
    public Result<List<ConversationVO>> list() {
        return Result.success(conversationApplicationService.list());
    }

    @GetMapping("/{id}")
    public Result<ConversationVO> detail(@PathVariable Long id) {
        return Result.success(conversationApplicationService.detail(id));
    }

    @GetMapping("/{id}/messages")
    public Result<List<MessageVO>> messages(@PathVariable Long id) {
        return Result.success(conversationApplicationService.listMessages(id));
    }

    @PostMapping("/{id}/messages")
    public Object sendMessage(@PathVariable Long id,
                              @Valid @RequestBody SendMessageRequest request,
                              HttpServletResponse response) {
        if (Boolean.TRUE.equals(request.stream())) {
            return conversationApplicationService.streamMessage(id, request, response);
        }
        return Result.success(conversationApplicationService.sendMessage(id, request));
    }

    @PostMapping("/{id}/messages/regenerate")
    public SseEmitter regenerateMessage(@PathVariable Long id,
                                        @RequestBody(required = false) RegenerateMessageRequest request,
                                        HttpServletResponse response) {
        Long platformModelId = request == null ? null : request.platformModelId();
        return conversationApplicationService.regenerateMessage(id, platformModelId, response);
    }

    @DeleteMapping("/{id}/messages/{messageId}")
    public Result<Void> deleteMessage(@PathVariable Long id, @PathVariable Long messageId) {
        conversationApplicationService.deleteMessage(id, messageId);
        return Result.success();
    }
}
