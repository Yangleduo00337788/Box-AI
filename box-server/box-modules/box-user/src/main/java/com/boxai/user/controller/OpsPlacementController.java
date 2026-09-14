package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.user.api.OpsPlacementVO;
import com.boxai.user.application.OpsPlacementApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ops/placements")
public class OpsPlacementController {

    private final OpsPlacementApplicationService opsPlacementApplicationService;

    public OpsPlacementController(OpsPlacementApplicationService opsPlacementApplicationService) {
        this.opsPlacementApplicationService = opsPlacementApplicationService;
    }

    @GetMapping
    public Result<List<OpsPlacementVO>> list(@RequestParam(required = false) String slot) {
        return Result.success(opsPlacementApplicationService.listActive(slot));
    }
}
