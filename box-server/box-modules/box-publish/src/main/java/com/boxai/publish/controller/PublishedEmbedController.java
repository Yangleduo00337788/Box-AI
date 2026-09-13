package com.boxai.publish.controller;

import com.boxai.agent.api.PublishedEmbedResolveVO;
import com.boxai.common.result.Result;
import com.boxai.publish.application.PublishedAgentApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/published/embed")
public class PublishedEmbedController {

    private final PublishedAgentApplicationService publishedAgentApplicationService;

    public PublishedEmbedController(PublishedAgentApplicationService publishedAgentApplicationService) {
        this.publishedAgentApplicationService = publishedAgentApplicationService;
    }

    @GetMapping("/resolve")
    public Result<PublishedEmbedResolveVO> resolve(HttpServletRequest request,
                                                   @RequestParam(required = false) String host) {
        String resolvedHost = host == null || host.isBlank() ? request.getServerName() : host;
        return Result.success(publishedAgentApplicationService.resolveByHost(resolvedHost));
    }
}
