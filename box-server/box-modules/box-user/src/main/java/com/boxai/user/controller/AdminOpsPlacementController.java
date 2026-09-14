package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.user.api.CreateOpsPlacementRequest;
import com.boxai.user.api.OpsPlacementVO;
import com.boxai.user.api.UpdateOpsPlacementRequest;
import com.boxai.user.application.OpsPlacementApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/ops/placements")
public class AdminOpsPlacementController {

    private final OpsPlacementApplicationService opsPlacementApplicationService;

    public AdminOpsPlacementController(OpsPlacementApplicationService opsPlacementApplicationService) {
        this.opsPlacementApplicationService = opsPlacementApplicationService;
    }

    @GetMapping
    public Result<List<OpsPlacementVO>> list() {
        return Result.success(opsPlacementApplicationService.listForAdmin());
    }

    @PostMapping
    public Result<OpsPlacementVO> create(@Valid @RequestBody CreateOpsPlacementRequest request) {
        return Result.success(opsPlacementApplicationService.create(request));
    }

    @PutMapping("/{id}")
    public Result<OpsPlacementVO> update(@PathVariable Long id, @Valid @RequestBody UpdateOpsPlacementRequest request) {
        return Result.success(opsPlacementApplicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        opsPlacementApplicationService.delete(id);
        return Result.success(null);
    }
}
