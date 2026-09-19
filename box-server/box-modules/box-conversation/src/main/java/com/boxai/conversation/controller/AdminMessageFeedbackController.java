package com.boxai.conversation.controller;

import com.boxai.common.result.PageResult;
import com.boxai.common.result.Result;
import com.boxai.conversation.api.AdminMessageFeedbackDetailVO;
import com.boxai.conversation.api.AdminMessageFeedbackReplyRequest;
import com.boxai.conversation.api.AdminMessageFeedbackVO;
import com.boxai.conversation.application.AdminMessageFeedbackApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/message-feedbacks")
public class AdminMessageFeedbackController {

    private final AdminMessageFeedbackApplicationService adminMessageFeedbackApplicationService;

    public AdminMessageFeedbackController(AdminMessageFeedbackApplicationService adminMessageFeedbackApplicationService) {
        this.adminMessageFeedbackApplicationService = adminMessageFeedbackApplicationService;
    }

    @GetMapping
    public Result<PageResult<AdminMessageFeedbackVO>> page(@RequestParam(required = false) String rating,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(adminMessageFeedbackApplicationService.page(rating, status, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<AdminMessageFeedbackDetailVO> detail(@PathVariable Long id) {
        return Result.success(adminMessageFeedbackApplicationService.detail(id));
    }

    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody AdminMessageFeedbackReplyRequest request) {
        adminMessageFeedbackApplicationService.reply(id, request);
        return Result.success();
    }
}
