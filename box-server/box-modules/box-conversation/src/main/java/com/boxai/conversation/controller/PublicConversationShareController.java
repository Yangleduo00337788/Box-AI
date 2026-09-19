package com.boxai.conversation.controller;

import com.boxai.common.result.Result;
import com.boxai.conversation.api.SharedConversationVO;
import com.boxai.conversation.application.ConversationShareApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/conversation-shares")
public class PublicConversationShareController {

    private final ConversationShareApplicationService conversationShareApplicationService;

    public PublicConversationShareController(ConversationShareApplicationService conversationShareApplicationService) {
        this.conversationShareApplicationService = conversationShareApplicationService;
    }

    @GetMapping("/{token}")
    public Result<SharedConversationVO> detail(@PathVariable String token) {
        return Result.success(conversationShareApplicationService.getPublicShare(token));
    }
}
