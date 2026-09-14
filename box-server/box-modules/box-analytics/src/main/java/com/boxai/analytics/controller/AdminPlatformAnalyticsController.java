package com.boxai.analytics.controller;

import com.boxai.analytics.api.AnalyticsTrendsVO;
import com.boxai.analytics.api.PlatformAnalyticsOverviewVO;
import com.boxai.analytics.application.AdminPlatformAnalyticsApplicationService;
import com.boxai.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AdminPlatformAnalyticsController {

    private final AdminPlatformAnalyticsApplicationService adminPlatformAnalyticsApplicationService;

    public AdminPlatformAnalyticsController(
            AdminPlatformAnalyticsApplicationService adminPlatformAnalyticsApplicationService) {
        this.adminPlatformAnalyticsApplicationService = adminPlatformAnalyticsApplicationService;
    }

    @GetMapping("/overview")
    public Result<PlatformAnalyticsOverviewVO> overview(@RequestParam(defaultValue = "7") int days) {
        return Result.success(adminPlatformAnalyticsApplicationService.overview(days));
    }

    @GetMapping("/trends")
    public Result<AnalyticsTrendsVO> trends(@RequestParam(defaultValue = "7") int days) {
        return Result.success(adminPlatformAnalyticsApplicationService.trends(days));
    }
}
