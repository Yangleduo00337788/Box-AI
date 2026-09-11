package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.LoginUser;
import com.boxai.security.context.SecurityContexts;
import com.boxai.tenant.api.QuotaSnapshotVO;
import com.boxai.tenant.application.QuotaApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant/quota")
public class QuotaController {

    private final QuotaApplicationService quotaApplicationService;

    public QuotaController(QuotaApplicationService quotaApplicationService) {
        this.quotaApplicationService = quotaApplicationService;
    }

    @GetMapping
    public Result<QuotaSnapshotVO> mine() {
        LoginUser user = SecurityContexts.currentUser();
        return Result.success(quotaApplicationService.getQuotaForUser(user.userId()));
    }
}
