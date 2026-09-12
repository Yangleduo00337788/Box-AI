package com.boxai.agent.application;



import com.boxai.domain.knowledge.KnowledgeBase;

import com.boxai.domain.knowledge.KnowledgeBaseRepository;

import com.boxai.domain.knowledge.KnowledgeChunkRepository;

import com.boxai.domain.knowledge.KnowledgeDocumentRepository;

import com.boxai.domain.mcp.McpServer;

import com.boxai.domain.mcp.McpServerRepository;

import com.boxai.domain.plugin.PluginCatalog;

import com.boxai.domain.plugin.WorkspacePluginInstall;

import com.boxai.domain.tool.Tool;

import com.boxai.domain.tool.ToolHttpConfig;

import com.boxai.domain.tool.ToolHttpConfigRepository;

import com.boxai.domain.tool.ToolRepository;

import com.boxai.domain.workflow.Workflow;

import com.boxai.domain.workflow.WorkflowRepository;

import com.boxai.domain.workflow.WorkflowVersion;

import com.boxai.domain.workflow.WorkflowVersionRepository;

import com.boxai.tenant.application.QuotaApplicationService;

import org.springframework.stereotype.Service;



@Service

public class PluginInstallProvisioner {



    private static final String DEFAULT_WORKFLOW_DEFINITION = """

            {"nodes":[{"id":"start","type":"Start","label":"开始","config":{}},{"id":"output-1","type":"Output","label":"输出","config":{}}],"edges":[{"id":"e-start-output","source":"start","target":"output-1"}],"variables":[]}

            """.trim();



    public record ProvisionResult(String resourceType, Long resourceId) {

    }



    private final ToolRepository toolRepository;

    private final ToolHttpConfigRepository toolHttpConfigRepository;

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    private final KnowledgeChunkRepository knowledgeChunkRepository;

    private final WorkflowRepository workflowRepository;

    private final WorkflowVersionRepository workflowVersionRepository;

    private final McpServerRepository mcpServerRepository;

    private final QuotaApplicationService quotaApplicationService;



    public PluginInstallProvisioner(ToolRepository toolRepository,

                                    ToolHttpConfigRepository toolHttpConfigRepository,

                                    KnowledgeBaseRepository knowledgeBaseRepository,

                                    KnowledgeDocumentRepository knowledgeDocumentRepository,

                                    KnowledgeChunkRepository knowledgeChunkRepository,

                                    WorkflowRepository workflowRepository,

                                    WorkflowVersionRepository workflowVersionRepository,

                                    McpServerRepository mcpServerRepository,

                                    QuotaApplicationService quotaApplicationService) {

        this.toolRepository = toolRepository;

        this.toolHttpConfigRepository = toolHttpConfigRepository;

        this.knowledgeBaseRepository = knowledgeBaseRepository;

        this.knowledgeDocumentRepository = knowledgeDocumentRepository;

        this.knowledgeChunkRepository = knowledgeChunkRepository;

        this.workflowRepository = workflowRepository;

        this.workflowVersionRepository = workflowVersionRepository;

        this.mcpServerRepository = mcpServerRepository;

        this.quotaApplicationService = quotaApplicationService;

    }



    public ProvisionResult provision(PluginCatalog plugin, Long workspaceId, Long userId) {

        String category = plugin.getCategory() == null ? "" : plugin.getCategory().trim().toLowerCase();

        if ("tools".equals(category)) {

            return provisionTool(plugin, workspaceId, userId);

        }

        if ("knowledge".equals(category)) {

            return provisionKnowledge(plugin, workspaceId, userId);

        }

        if ("workflows".equals(category)) {

            return provisionWorkflow(plugin, workspaceId, userId);

        }

        if ("mcp".equals(category)) {

            return provisionMcp(plugin, workspaceId, userId);

        }

        return null;

    }



    public void deprovision(WorkspacePluginInstall install) {

        if (install.getResourceType() == null || install.getResourceId() == null) {

            return;

        }

        if ("tool".equals(install.getResourceType())) {

            deprovisionTool(install.getResourceId());

            return;

        }

        if ("knowledge".equals(install.getResourceType())) {

            deprovisionKnowledge(install.getResourceId());

            return;

        }

        if ("workflow".equals(install.getResourceType())) {

            deprovisionWorkflow(install.getResourceId());

            return;

        }

        if ("mcp".equals(install.getResourceType())) {

            mcpServerRepository.delete(install.getResourceId());

        }

    }



    private ProvisionResult provisionTool(PluginCatalog plugin, Long workspaceId, Long userId) {

        Tool tool = new Tool();

        tool.setWorkspaceId(workspaceId);

        tool.setName(plugin.getTitle());

        tool.setToolKey("plugin-" + plugin.getPluginCode());

        tool.setDescription(plugin.getDescription());

        tool.setType("HTTP");

        tool.setStatus(1);

        tool.setCreatedBy(userId);

        toolRepository.save(tool);



        ToolHttpConfig config = new ToolHttpConfig();

        config.setToolId(tool.getId());

        config.setMethod("GET");

        config.setUrl("https://httpbin.org/get");

        config.setTimeoutMs(10000);

        config.setAllowRedirect(false);

        toolHttpConfigRepository.save(config);

        return new ProvisionResult("tool", tool.getId());

    }



    private ProvisionResult provisionKnowledge(PluginCatalog plugin, Long workspaceId, Long userId) {

        quotaApplicationService.assertKnowledgeBaseQuotaAvailable(workspaceId);

        KnowledgeBase kb = new KnowledgeBase();

        kb.setWorkspaceId(workspaceId);

        kb.setName(plugin.getTitle());

        kb.setDescription(plugin.getDescription());

        kb.setDocumentCount(0);

        kb.setChunkCount(0L);

        kb.setStatus("READY");

        kb.setCreatedBy(userId);

        knowledgeBaseRepository.save(kb);

        return new ProvisionResult("knowledge", kb.getId());

    }



    private ProvisionResult provisionWorkflow(PluginCatalog plugin, Long workspaceId, Long userId) {

        Workflow workflow = new Workflow();

        workflow.setWorkspaceId(workspaceId);

        workflow.setName(plugin.getTitle());

        workflow.setDescription(plugin.getDescription());

        workflow.setStatus("DRAFT");

        workflow.setCreatedBy(userId);

        workflowRepository.save(workflow);



        WorkflowVersion version = new WorkflowVersion();

        version.setWorkflowId(workflow.getId());

        version.setWorkspaceId(workspaceId);

        version.setVersionNo(1);

        version.setStatus("DRAFT");

        version.setDefinitionJson(DEFAULT_WORKFLOW_DEFINITION);

        version.setCreatedBy(userId);

        workflowVersionRepository.save(version);



        workflow.setDraftVersionId(version.getId());

        workflowRepository.update(workflow);

        return new ProvisionResult("workflow", workflow.getId());

    }



    private ProvisionResult provisionMcp(PluginCatalog plugin, Long workspaceId, Long userId) {

        McpServer server = new McpServer();

        server.setWorkspaceId(workspaceId);

        server.setName(plugin.getTitle());

        server.setServerKey("plugin-" + plugin.getPluginCode());

        server.setDescription(plugin.getDescription());

        server.setTransportType("HTTP");

        server.setEndpointUrl("https://example.com/mcp");

        server.setAuthType("NONE");

        server.setToolCatalogJson("[]");

        server.setStatus(1);

        server.setCreatedBy(userId);

        mcpServerRepository.save(server);

        return new ProvisionResult("mcp", server.getId());

    }



    private void deprovisionTool(Long toolId) {

        toolHttpConfigRepository.deleteByToolId(toolId);

        toolRepository.delete(toolId);

    }



    private void deprovisionKnowledge(Long knowledgeBaseId) {

        knowledgeDocumentRepository.listByKnowledgeBase(knowledgeBaseId).forEach(doc -> {

            knowledgeChunkRepository.deleteByDocument(doc.getId());

            knowledgeDocumentRepository.delete(doc.getId());

        });

        knowledgeBaseRepository.delete(knowledgeBaseId);

    }



    private void deprovisionWorkflow(Long workflowId) {

        workflowVersionRepository.deleteByWorkflowId(workflowId);

        workflowRepository.delete(workflowId);

    }

}


