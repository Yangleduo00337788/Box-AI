package com.boxai.agent.controller;

import com.boxai.agent.api.AgentChatRequest;
import com.boxai.agent.api.AgentKnowledgeBindingVO;
import com.boxai.agent.api.AgentLongTermMemoryVO;
import com.boxai.agent.api.AgentMcpBindingVO;
import com.boxai.agent.api.AgentPublishVO;
import com.boxai.agent.api.AgentSubAgentBindingVO;
import com.boxai.agent.api.AgentToolBindingVO;
import com.boxai.agent.api.AgentVersionCompareVO;
import com.boxai.agent.api.AgentVersionVO;
import com.boxai.agent.api.AgentVO;
import com.boxai.agent.api.BindAgentKnowledgeRequest;
import com.boxai.agent.api.BindAgentMcpRequest;
import com.boxai.agent.api.BindAgentSubAgentRequest;
import com.boxai.agent.api.BindAgentToolRequest;
import com.boxai.agent.api.CreateAgentRequest;
import com.boxai.agent.api.CreateAgentVersionRequest;
import com.boxai.agent.api.AgentEmbedConfigVO;
import com.boxai.agent.api.UpdateAgentConfigRequest;
import com.boxai.agent.api.UpdateAgentEmbedConfigRequest;
import com.boxai.agent.api.UpdateAgentMemoryRequest;
import com.boxai.agent.api.UpdateAgentModelRequest;
import com.boxai.agent.api.UpdateAgentPromptRequest;
import com.boxai.agent.api.UpdateAgentRequest;
import com.boxai.agent.application.AgentApplicationService;
import com.boxai.agent.application.AgentBindingApplicationService;
import com.boxai.agent.application.AgentLongTermMemoryApplicationService;
import com.boxai.agent.application.AgentPublishApplicationService;
import com.boxai.agent.application.AgentVersionApplicationService;
import com.boxai.common.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AgentApplicationService agentApplicationService;
    private final AgentBindingApplicationService agentBindingApplicationService;
    private final AgentPublishApplicationService agentPublishApplicationService;
    private final AgentVersionApplicationService agentVersionApplicationService;
    private final AgentLongTermMemoryApplicationService agentLongTermMemoryApplicationService;

    public AgentController(AgentApplicationService agentApplicationService,
                           AgentBindingApplicationService agentBindingApplicationService,
                           AgentPublishApplicationService agentPublishApplicationService,
                           AgentVersionApplicationService agentVersionApplicationService,
                           AgentLongTermMemoryApplicationService agentLongTermMemoryApplicationService) {
        this.agentApplicationService = agentApplicationService;
        this.agentBindingApplicationService = agentBindingApplicationService;
        this.agentPublishApplicationService = agentPublishApplicationService;
        this.agentVersionApplicationService = agentVersionApplicationService;
        this.agentLongTermMemoryApplicationService = agentLongTermMemoryApplicationService;
    }

    @GetMapping
    public Result<List<AgentVO>> list() {
        return Result.success(agentApplicationService.list());
    }

    @GetMapping("/{id}")
    public Result<AgentVO> detail(@PathVariable Long id) {
        return Result.success(agentApplicationService.detail(id));
    }

    @PostMapping
    public Result<AgentVO> create(@Valid @RequestBody CreateAgentRequest request) {
        return Result.success(agentApplicationService.create(request));
    }

    @PutMapping("/{id}")
    public Result<AgentVO> update(@PathVariable Long id, @Valid @RequestBody UpdateAgentRequest request) {
        return Result.success(agentApplicationService.update(id, request));
    }

    @PutMapping("/{id}/prompt")
    public Result<AgentVO> updatePrompt(@PathVariable Long id, @Valid @RequestBody UpdateAgentPromptRequest request) {
        return Result.success(agentApplicationService.updatePrompt(id, request));
    }

    @PutMapping("/{id}/model")
    public Result<AgentVO> updateModel(@PathVariable Long id, @Valid @RequestBody UpdateAgentModelRequest request) {
        return Result.success(agentApplicationService.updateModelConfig(id, request));
    }

    @PutMapping("/{id}/memory")
    public Result<AgentVO> updateMemory(@PathVariable Long id, @Valid @RequestBody UpdateAgentMemoryRequest request) {
        return Result.success(agentApplicationService.updateMemoryConfig(id, request));
    }

    @GetMapping("/{id}/long-term-memories")
    public Result<List<AgentLongTermMemoryVO>> listLongTermMemories(@PathVariable Long id) {
        return Result.success(agentLongTermMemoryApplicationService.list(id));
    }

    @DeleteMapping("/{id}/long-term-memories/{memoryId}")
    public Result<Void> deleteLongTermMemory(@PathVariable Long id, @PathVariable Long memoryId) {
        agentLongTermMemoryApplicationService.delete(id, memoryId);
        return Result.success();
    }

    @PutMapping("/{id}/config")
    public Result<AgentVO> updateConfig(@PathVariable Long id, @Valid @RequestBody UpdateAgentConfigRequest request) {
        return Result.success(agentApplicationService.updateConfig(id, request));
    }

    @GetMapping("/{id}/embed-config")
    public Result<AgentEmbedConfigVO> getEmbedConfig(@PathVariable Long id) {
        return Result.success(agentApplicationService.getEmbedConfig(id));
    }

    @PutMapping("/{id}/embed-config")
    public Result<AgentEmbedConfigVO> updateEmbedConfig(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateAgentEmbedConfigRequest request) {
        return Result.success(agentApplicationService.updateEmbedConfig(id, request));
    }

    @PostMapping("/{id}/embed-domain/verify")
    public Result<AgentEmbedConfigVO> verifyEmbedDomain(@PathVariable Long id) {
        return Result.success(agentApplicationService.verifyEmbedDomain(id));
    }

    @PostMapping("/{id}/chat")
    public Object chat(@PathVariable Long id,
                       @Valid @RequestBody AgentChatRequest request,
                       HttpServletResponse response) {
        if (Boolean.TRUE.equals(request.stream())) {
            return agentApplicationService.streamChat(id, request, response);
        }
        return Result.success(agentApplicationService.chat(id, request));
    }

    @PostMapping("/{id}/tools/confirm")
    public Result<com.boxai.agent.api.AgentToolConfirmVO> confirmTool(@PathVariable Long id,
                                                                      @Valid @RequestBody com.boxai.agent.api.ConfirmAgentToolRequest request) {
        return Result.success(agentApplicationService.confirmTool(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        agentApplicationService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/knowledge")
    public Result<List<AgentKnowledgeBindingVO>> listKnowledge(@PathVariable Long id) {
        return Result.success(agentBindingApplicationService.listKnowledge(id));
    }

    @PostMapping("/{id}/knowledge")
    public Result<AgentKnowledgeBindingVO> bindKnowledge(@PathVariable Long id,
                                                       @Valid @RequestBody BindAgentKnowledgeRequest request) {
        return Result.success(agentBindingApplicationService.bindKnowledge(id, request));
    }

    @DeleteMapping("/{id}/knowledge/{knowledgeId}")
    public Result<Void> unbindKnowledge(@PathVariable Long id, @PathVariable Long knowledgeId) {
        agentBindingApplicationService.unbindKnowledge(id, knowledgeId);
        return Result.success();
    }

    @GetMapping("/{id}/workflows")
    public Result<List<com.boxai.agent.api.AgentWorkflowBindingVO>> listWorkflows(@PathVariable Long id) {
        return Result.success(agentBindingApplicationService.listWorkflows(id));
    }

    @PostMapping("/{id}/workflows")
    public Result<com.boxai.agent.api.AgentWorkflowBindingVO> bindWorkflow(@PathVariable Long id,
                                                                           @Valid @RequestBody com.boxai.agent.api.BindAgentWorkflowRequest request) {
        return Result.success(agentBindingApplicationService.bindWorkflow(id, request));
    }

    @DeleteMapping("/{id}/workflows/{workflowId}")
    public Result<Void> unbindWorkflow(@PathVariable Long id, @PathVariable Long workflowId) {
        agentBindingApplicationService.unbindWorkflow(id, workflowId);
        return Result.success();
    }

    @GetMapping("/{id}/tools")
    public Result<List<AgentToolBindingVO>> listTools(@PathVariable Long id) {
        return Result.success(agentBindingApplicationService.listTools(id));
    }

    @PostMapping("/{id}/tools")
    public Result<AgentToolBindingVO> bindTool(@PathVariable Long id,
                                               @Valid @RequestBody BindAgentToolRequest request) {
        return Result.success(agentBindingApplicationService.bindTool(id, request));
    }

    @PutMapping("/{id}/tools/{toolId}")
    public Result<com.boxai.agent.api.AgentToolBindingVO> updateTool(@PathVariable Long id,
                                                                     @PathVariable Long toolId,
                                                                     @RequestBody com.boxai.agent.api.UpdateAgentToolRequest request) {
        return Result.success(agentBindingApplicationService.updateTool(id, toolId, request));
    }

    @DeleteMapping("/{id}/tools/{toolId}")
    public Result<Void> unbindTool(@PathVariable Long id, @PathVariable Long toolId) {
        agentBindingApplicationService.unbindTool(id, toolId);
        return Result.success();
    }

    @GetMapping("/{id}/mcp")
    public Result<List<AgentMcpBindingVO>> listMcp(@PathVariable Long id) {
        return Result.success(agentBindingApplicationService.listMcp(id));
    }

    @PostMapping("/{id}/mcp")
    public Result<AgentMcpBindingVO> bindMcp(@PathVariable Long id, @Valid @RequestBody BindAgentMcpRequest request) {
        return Result.success(agentBindingApplicationService.bindMcp(id, request));
    }

    @DeleteMapping("/{id}/mcp/{mcpServerId}")
    public Result<Void> unbindMcp(@PathVariable Long id, @PathVariable Long mcpServerId) {
        agentBindingApplicationService.unbindMcp(id, mcpServerId);
        return Result.success();
    }

    @GetMapping("/{id}/sub-agents")
    public Result<List<AgentSubAgentBindingVO>> listSubAgents(@PathVariable Long id) {
        return Result.success(agentBindingApplicationService.listSubAgents(id));
    }

    @PostMapping("/{id}/sub-agents")
    public Result<AgentSubAgentBindingVO> bindSubAgent(@PathVariable Long id,
                                                       @Valid @RequestBody BindAgentSubAgentRequest request) {
        return Result.success(agentBindingApplicationService.bindSubAgent(id, request));
    }

    @DeleteMapping("/{id}/sub-agents/{subAgentId}")
    public Result<Void> unbindSubAgent(@PathVariable Long id, @PathVariable Long subAgentId) {
        agentBindingApplicationService.unbindSubAgent(id, subAgentId);
        return Result.success();
    }

    @GetMapping("/{id}/publish")
    public Result<AgentPublishVO> publishStatus(@PathVariable Long id) {
        return Result.success(agentPublishApplicationService.getPublishStatus(id));
    }

    @PostMapping("/{id}/publish")
    public Result<AgentPublishVO> publish(@PathVariable Long id) {
        return Result.success(agentPublishApplicationService.publish(id));
    }

    @PostMapping("/{id}/unpublish")
    public Result<AgentPublishVO> unpublish(@PathVariable Long id) {
        return Result.success(agentPublishApplicationService.unpublish(id));
    }

    @GetMapping("/{id}/versions")
    public Result<List<AgentVersionVO>> listVersions(@PathVariable Long id) {
        return Result.success(agentVersionApplicationService.list(id));
    }

    @GetMapping("/{id}/versions/compare")
    public Result<AgentVersionCompareVO> compareVersions(@PathVariable Long id,
                                                         @RequestParam Long baseId,
                                                         @RequestParam Long targetId) {
        return Result.success(agentVersionApplicationService.compare(id, baseId, targetId));
    }

    @PostMapping("/{id}/versions")
    public Result<AgentVersionVO> createVersion(@PathVariable Long id,
                                               @RequestBody(required = false) CreateAgentVersionRequest request) {
        return Result.success(agentVersionApplicationService.createSnapshot(id, request));
    }

    @PostMapping("/{id}/versions/{versionId}/restore")
    public Result<Void> restoreVersion(@PathVariable Long id, @PathVariable Long versionId) {
        agentVersionApplicationService.restore(id, versionId);
        return Result.success();
    }

    @PostMapping("/{id}/versions/{versionId}/archive")
    public Result<AgentVersionVO> archiveVersion(@PathVariable Long id, @PathVariable Long versionId) {
        return Result.success(agentVersionApplicationService.archive(id, versionId));
    }

    @PostMapping("/{id}/duplicate")
    public Result<AgentVO> duplicate(@PathVariable Long id) {
        return Result.success(agentApplicationService.duplicate(id));
    }

    @PostMapping("/{id}/archive")
    public Result<AgentVO> archive(@PathVariable Long id) {
        return Result.success(agentApplicationService.archive(id));
    }
}
