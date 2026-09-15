package com.boxai.conversation.controller;

import com.boxai.common.result.Result;
import com.boxai.conversation.api.ChatProjectVO;
import com.boxai.conversation.api.CreateChatProjectRequest;
import com.boxai.conversation.api.RenameChatProjectRequest;
import com.boxai.conversation.application.ChatProjectApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ChatProjectController {

    private final ChatProjectApplicationService chatProjectApplicationService;

    public ChatProjectController(ChatProjectApplicationService chatProjectApplicationService) {
        this.chatProjectApplicationService = chatProjectApplicationService;
    }

    @GetMapping
    public Result<List<ChatProjectVO>> list() {
        return Result.success(chatProjectApplicationService.list());
    }

    @PostMapping
    public Result<ChatProjectVO> create(@Valid @RequestBody CreateChatProjectRequest request) {
        return Result.success(chatProjectApplicationService.create(request));
    }

    @PutMapping("/{id}")
    public Result<ChatProjectVO> rename(@PathVariable Long id, @Valid @RequestBody RenameChatProjectRequest request) {
        return Result.success(chatProjectApplicationService.rename(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        chatProjectApplicationService.delete(id);
        return Result.success();
    }
}
