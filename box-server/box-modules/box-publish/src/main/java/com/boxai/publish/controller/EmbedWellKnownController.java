package com.boxai.publish.controller;

import com.boxai.agent.application.EmbedDomainApplicationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class EmbedWellKnownController {

    private final EmbedDomainApplicationService embedDomainApplicationService;

    public EmbedWellKnownController(EmbedDomainApplicationService embedDomainApplicationService) {
        this.embedDomainApplicationService = embedDomainApplicationService;
    }

    @GetMapping(value = "/.well-known/box-domain-verify.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyFile(HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        return embedDomainApplicationService.findVerifyTokenByHost(host)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
