package com.boxai.runtime.controller;

import com.boxai.common.result.Result;
import com.boxai.runtime.api.WorkflowExecutionResultVO;
import com.boxai.runtime.application.WorkflowWebhookApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/hooks/workflows")
public class WorkflowWebhookController {

    private final WorkflowWebhookApplicationService workflowWebhookApplicationService;

    public WorkflowWebhookController(WorkflowWebhookApplicationService workflowWebhookApplicationService) {
        this.workflowWebhookApplicationService = workflowWebhookApplicationService;
    }

    @PostMapping("/{token}")
    public Result<WorkflowExecutionResultVO> trigger(@PathVariable String token,
                                                     @RequestBody(required = false) String body,
                                                     HttpServletRequest request) throws IOException {
        String payload = body;
        if (payload == null) {
            payload = readBody(request);
        }
        String signature = request.getHeader("X-Box-Signature");
        return Result.success(workflowWebhookApplicationService.trigger(token, payload, signature));
    }

    private String readBody(HttpServletRequest request) throws IOException {
        byte[] bytes = request.getInputStream().readAllBytes();
        return bytes.length == 0 ? "" : new String(bytes, StandardCharsets.UTF_8);
    }
}
