package com.boxai.analytics.controller;

import com.boxai.analytics.api.AnalyticsOverviewVO;
import com.boxai.analytics.api.AnalyticsTrendsVO;
import com.boxai.analytics.application.AnalyticsApplicationService;
import com.boxai.common.result.Result;
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

    @GetMapping("/trends")
    public Result<AnalyticsTrendsVO> trends(@RequestParam(defaultValue = "7") int days) {
        return Result.success(analyticsApplicationService.trends(days));
    }
}
