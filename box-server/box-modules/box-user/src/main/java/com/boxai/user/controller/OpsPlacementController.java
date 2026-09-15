package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.user.api.OpsPlacementVO;
import com.boxai.user.application.OpsMetricsApplicationService;
import com.boxai.user.application.OpsPlacementApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ops/placements")
public class OpsPlacementController {

    private final OpsPlacementApplicationService opsPlacementApplicationService;
    private final OpsMetricsApplicationService opsMetricsApplicationService;

    public OpsPlacementController(OpsPlacementApplicationService opsPlacementApplicationService,
                                  OpsMetricsApplicationService opsMetricsApplicationService) {
        this.opsPlacementApplicationService = opsPlacementApplicationService;
        this.opsMetricsApplicationService = opsMetricsApplicationService;
    }

    @GetMapping
    public Result<List<OpsPlacementVO>> list(@RequestParam(required = false) String slot) {
        return Result.success(opsPlacementApplicationService.listActive(slot));
    }

    @PostMapping("/{id}/track")
    public Result<Void> track(@PathVariable Long id, @RequestParam(defaultValue = "impression") String event) {
        opsMetricsApplicationService.track(id, event);
        return Result.success(null);
    }
}
