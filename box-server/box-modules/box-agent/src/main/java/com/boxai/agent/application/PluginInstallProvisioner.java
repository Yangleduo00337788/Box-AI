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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public PluginInstallProvisioner(ToolRepository toolRepository,
                                    ToolHttpConfigRepository toolHttpConfigRepository,
                                    KnowledgeBaseRepository knowledgeBaseRepository,
                                    KnowledgeDocumentRepository knowledgeDocumentRepository,
                                    KnowledgeChunkRepository knowledgeChunkRepository,
                                    WorkflowRepository workflowRepository,
                                    WorkflowVersionRepository workflowVersionRepository,
                                    McpServerRepository mcpServerRepository,
                                    QuotaApplicationService quotaApplicationService,
                                    ObjectMapper objectMapper) {
        this.toolRepository = toolRepository;
        this.toolHttpConfigRepository = toolHttpConfigRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.workflowRepository = workflowRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.mcpServerRepository = mcpServerRepository;
        this.quotaApplicationService = quotaApplicationService;
        this.objectMapper = objectMapper;
    }

    public ProvisionResult provision(PluginCatalog plugin, Long workspaceId, Long userId) {
        String category = normalizeCategory(plugin.getCategory());
        JsonNode manifest = PluginManifest.parse(plugin.getManifestJson(), objectMapper);
        if ("tools".equals(category)) {
            return provisionTool(plugin, manifest, workspaceId, userId);
        }
        if ("knowledge".equals(category) || "skills".equals(category)) {
            return provisionKnowledge(plugin, manifest, workspaceId, userId);
        }
        if ("workflows".equals(category)) {
            return provisionWorkflow(plugin, manifest, workspaceId, userId);
        }
        if ("mcp".equals(category)) {
            return provisionMcp(plugin, manifest, workspaceId, userId);
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

    private ProvisionResult provisionTool(PluginCatalog plugin, JsonNode manifest, Long workspaceId, Long userId) {
        String url = PluginManifest.text(manifest, "url", "https://httpbin.org/get");
        Tool tool = new Tool();
        tool.setWorkspaceId(workspaceId);
        tool.setName(plugin.getTitle());
        tool.setToolKey("plugin-" + plugin.getPluginCode());
        tool.setDescription(plugin.getDescription());
        tool.setType(PluginManifest.text(manifest, "type", "HTTP"));
        tool.setStatus(1);
        tool.setCreatedBy(userId);
        toolRepository.save(tool);

        ToolHttpConfig config = new ToolHttpConfig();
        config.setToolId(tool.getId());
        config.setMethod(PluginManifest.text(manifest, "method", "GET").toUpperCase());
        config.setUrl(url);
        config.setTimeoutMs(PluginManifest.intValue(manifest, "timeoutMs", 10000));
        config.setAllowRedirect(PluginManifest.boolValue(manifest, "allowRedirect", false));
        toolHttpConfigRepository.save(config);
        return new ProvisionResult("tool", tool.getId());
    }

    private ProvisionResult provisionKnowledge(PluginCatalog plugin, JsonNode manifest, Long workspaceId, Long userId) {
        quotaApplicationService.assertKnowledgeBaseQuotaAvailable(workspaceId);
        KnowledgeBase kb = new KnowledgeBase();
        kb.setWorkspaceId(workspaceId);
        kb.setName(plugin.getTitle());
        String instructions = PluginManifest.text(manifest, "instructions", plugin.getDescription());
        kb.setDescription(instructions);
        kb.setDocumentCount(0);
        kb.setChunkCount(0L);
        kb.setStatus("READY");
        kb.setCreatedBy(userId);
        knowledgeBaseRepository.save(kb);
        return new ProvisionResult("knowledge", kb.getId());
    }

    private ProvisionResult provisionWorkflow(PluginCatalog plugin, JsonNode manifest, Long workspaceId, Long userId) {
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
        version.setDefinitionJson(
                PluginManifest.resolveWorkflowDefinition(manifest, objectMapper, DEFAULT_WORKFLOW_DEFINITION));
        version.setCreatedBy(userId);
        workflowVersionRepository.save(version);

        workflow.setDraftVersionId(version.getId());
        workflowRepository.update(workflow);
        return new ProvisionResult("workflow", workflow.getId());
    }

    private ProvisionResult provisionMcp(PluginCatalog plugin, JsonNode manifest, Long workspaceId, Long userId) {
        String endpointUrl = PluginManifest.text(manifest, "endpointUrl", "https://example.com/mcp");
        McpServer server = new McpServer();
        server.setWorkspaceId(workspaceId);
        server.setName(plugin.getTitle());
        server.setServerKey("plugin-" + plugin.getPluginCode());
        server.setDescription(plugin.getDescription());
        server.setTransportType(PluginManifest.text(manifest, "transportType", "HTTP"));
        server.setEndpointUrl(endpointUrl);
        server.setAuthType(PluginManifest.text(manifest, "authType", "NONE"));
        server.setToolCatalogJson(PluginManifest.text(manifest, "toolCatalogJson", "[]"));
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

    private static String normalizeCategory(String category) {
        if (category == null) {
            return "";
        }
        String key = category.trim().toLowerCase();
        if ("workflow".equals(key)) {
            return "workflows";
        }
        return key;
    }
}
