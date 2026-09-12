package com.boxai.conversation.controller;

import com.boxai.common.result.Result;
import com.boxai.conversation.api.AnalyticsOverviewVO;
import com.boxai.conversation.application.AnalyticsApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsApplicationService analyticsApplicationService;

    public AnalyticsController(AnalyticsApplicationService analyticsApplicationService) {
        this.analyticsApplicationService = analyticsApplicationService;
    }

    @GetMapping("/overview")
    public Result<AnalyticsOverviewVO> overview(@RequestParam(defaultValue = "7") int days) {
        return Result.success(analyticsApplicationService.overview(days));
    }
}
