package com.boxai.agent.controller;

import com.boxai.agent.api.template.AgentTemplateVO;
import com.boxai.agent.api.template.CreateAgentTemplateRequest;
import com.boxai.agent.api.template.UpdateAgentTemplateRequest;
import com.boxai.agent.api.template.UpdateAgentTemplateStatusRequest;
import com.boxai.agent.application.AgentTemplateApplicationService;
import com.boxai.common.result.Result;
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
@RequestMapping("/api/v1/admin/agent-templates")
public class AdminAgentTemplateController {

    private final AgentTemplateApplicationService agentTemplateApplicationService;

    public AdminAgentTemplateController(AgentTemplateApplicationService agentTemplateApplicationService) {
        this.agentTemplateApplicationService = agentTemplateApplicationService;
    }

    @GetMapping
    public Result<List<AgentTemplateVO>> list() {
        return Result.success(agentTemplateApplicationService.listAdmin());
    }

    @PostMapping
    public Result<AgentTemplateVO> create(@Valid @RequestBody CreateAgentTemplateRequest request) {
        return Result.success(agentTemplateApplicationService.create(request));
    }

    @PutMapping("/{id}")
    public Result<AgentTemplateVO> update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateAgentTemplateRequest request) {
        return Result.success(agentTemplateApplicationService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public Result<AgentTemplateVO> updateStatus(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateAgentTemplateStatusRequest request) {
        return Result.success(agentTemplateApplicationService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        agentTemplateApplicationService.delete(id);
        return Result.success(null);
    }
}
