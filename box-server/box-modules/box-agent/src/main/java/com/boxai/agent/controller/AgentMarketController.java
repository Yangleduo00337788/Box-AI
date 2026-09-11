package com.boxai.agent.controller;

import com.boxai.agent.api.AgentVO;
import com.boxai.agent.api.template.AgentTemplateVO;
import com.boxai.agent.application.AgentTemplateApplicationService;
import com.boxai.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
public class AgentMarketController {

    private final AgentTemplateApplicationService agentTemplateApplicationService;

    public AgentMarketController(AgentTemplateApplicationService agentTemplateApplicationService) {
        this.agentTemplateApplicationService = agentTemplateApplicationService;
    }

    @GetMapping("/templates")
    public Result<List<AgentTemplateVO>> listTemplates() {
        return Result.success(agentTemplateApplicationService.listMarket());
    }

    @PostMapping("/templates/{id}/enable")
    public Result<AgentVO> enableTemplate(@PathVariable Long id) {
        return Result.success(agentTemplateApplicationService.enable(id));
    }
}
