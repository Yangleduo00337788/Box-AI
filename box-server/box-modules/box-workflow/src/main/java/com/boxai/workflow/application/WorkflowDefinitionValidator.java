package com.boxai.workflow.application;

import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.mcp.McpServer;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.domain.platform.PlatformModel;
import com.boxai.domain.platform.PlatformModelRepository;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.workflow.api.WorkflowValidateVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WorkflowDefinitionValidator {

    private static final String EMPTY_DEFINITION = "{\"nodes\":[],\"edges\":[],\"variables\":[]}";
    private static final Set<String> KNOWN_NODE_TYPES = Set.of(
            "Start", "Input", "Output", "LLM", "Agent", "Knowledge", "HTTP", "Webhook",
            "SubWorkflow", "Tool", "Condition", "Switch", "Delay", "Variable", "Template",
            "Loop", "Code", "Parallel");
    private static final Pattern TEMPLATE_VAR_PATTERN = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*\\}\\}");

    private final ObjectMapper objectMapper;
    private final AgentRepository agentRepository;
    private final ToolRepository toolRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final WorkflowRepository workflowRepository;
    private final PlatformModelRepository platformModelRepository;
    private final McpServerRepository mcpServerRepository;

    public WorkflowDefinitionValidator(ObjectMapper objectMapper,
                                       AgentRepository agentRepository,
                                       ToolRepository toolRepository,
                                       KnowledgeBaseRepository knowledgeBaseRepository,
                                       WorkflowRepository workflowRepository,
                                       PlatformModelRepository platformModelRepository,
                                       McpServerRepository mcpServerRepository) {
        this.objectMapper = objectMapper;
        this.agentRepository = agentRepository;
        this.toolRepository = toolRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.workflowRepository = workflowRepository;
        this.platformModelRepository = platformModelRepository;
        this.mcpServerRepository = mcpServerRepository;
    }

    public String normalizeDefinition(String definitionJson) {
        if (definitionJson == null || definitionJson.isBlank()) {
            return EMPTY_DEFINITION;
        }
        return definitionJson;
    }

    public WorkflowValidateVO validate(String definitionJson) {
        return validate(definitionJson, null);
    }

    public WorkflowValidateVO validate(String definitionJson, Long workspaceId) {
        List<String> errors = new ArrayList<>();
        JsonNode root;
        try {
            root = objectMapper.readTree(normalizeDefinition(definitionJson));
        } catch (Exception ex) {
            errors.add("工作流定义不是合法的 JSON");
            return new WorkflowValidateVO(false, errors);
        }
        if (!root.isObject()) {
            errors.add("工作流定义必须是 JSON 对象");
            return new WorkflowValidateVO(false, errors);
        }

        JsonNode nodes = root.get("nodes");
        JsonNode edges = root.get("edges");
        if (nodes == null || !nodes.isArray()) {
            errors.add("缺少 nodes 数组");
            return new WorkflowValidateVO(false, errors);
        }
        if (edges == null || !edges.isArray()) {
            errors.add("缺少 edges 数组");
            return new WorkflowValidateVO(false, errors);
        }

        Set<String> declaredVariables = collectDeclaredVariables(root.get("variables"));
        Set<String> outputVariables = new HashSet<>();
        Map<String, JsonNode> nodeMap = new HashMap<>();
        int startCount = 0;
        int outputCount = 0;
        for (JsonNode node : nodes) {
            if (!node.isObject()) {
                errors.add("nodes 中存在非法节点");
                continue;
            }
            JsonNode idNode = node.get("id");
            JsonNode typeNode = node.get("type");
            if (idNode == null || idNode.asText().isBlank()) {
                errors.add("存在未设置 id 的节点");
                continue;
            }
            String id = idNode.asText();
            if (nodeMap.containsKey(id)) {
                errors.add("存在重复节点 id: " + id);
                continue;
            }
            nodeMap.put(id, node);
            String type = typeNode == null ? "" : typeNode.asText();
            if (!type.isBlank() && !KNOWN_NODE_TYPES.contains(type)) {
                errors.add("未知节点类型: " + type + " (" + id + ")");
            }
            if ("Start".equalsIgnoreCase(type)) {
                startCount++;
            }
            if ("Output".equalsIgnoreCase(type)) {
                outputCount++;
            }
            JsonNode config = node.get("config");
            collectOutputVariable(config, outputVariables);
            validateNodeConfig(id, type, config, workspaceId, errors);
            validateTemplateReferences(id, config, declaredVariables, outputVariables, errors);
        }

        if (startCount == 0) {
            errors.add("必须包含一个 Start 节点");
        }
        if (startCount > 1) {
            errors.add("只能包含一个 Start 节点");
        }
        if (outputCount == 0) {
            errors.add("必须包含至少一个 Output 节点");
        }

        Set<String> connected = new HashSet<>();
        Map<String, List<String>> adjacency = new HashMap<>();
        Map<String, Set<String>> branchHandles = new HashMap<>();
        for (JsonNode edge : edges) {
            if (!edge.isObject()) {
                errors.add("edges 中存在非法连线");
                continue;
            }
            JsonNode sourceNode = edge.get("source");
            JsonNode targetNode = edge.get("target");
            if (sourceNode == null || targetNode == null) {
                errors.add("存在缺少 source/target 的连线");
                continue;
            }
            String source = sourceNode.asText();
            String target = targetNode.asText();
            if (!nodeMap.containsKey(source)) {
                errors.add("连线引用了不存在的源节点: " + source);
            }
            if (!nodeMap.containsKey(target)) {
                errors.add("连线引用了不存在的目标节点: " + target);
            }
            connected.add(source);
            connected.add(target);
            adjacency.computeIfAbsent(source, key -> new ArrayList<>()).add(target);
            JsonNode handleNode = edge.get("sourceHandle");
            if (handleNode != null && !handleNode.asText().isBlank()) {
                branchHandles.computeIfAbsent(source, key -> new HashSet<>()).add(handleNode.asText());
            }
        }

        for (String nodeId : nodeMap.keySet()) {
            JsonNode node = nodeMap.get(nodeId);
            String type = node.path("type").asText("");
            if ("Start".equalsIgnoreCase(type)) {
                continue;
            }
            if (!connected.contains(nodeId)) {
                errors.add("存在孤立节点: " + nodeId);
            }
            validateBranchNode(nodeId, type, node.get("config"), branchHandles.getOrDefault(nodeId, Set.of()), errors);
        }

        if (hasCycle(adjacency, nodeMap.keySet())) {
            errors.add("工作流存在环路");
        }

        return new WorkflowValidateVO(errors.isEmpty(), errors);
    }

    private Set<String> collectDeclaredVariables(JsonNode variables) {
        Set<String> names = new HashSet<>();
        names.add("input");
        if (variables == null || !variables.isArray()) {
            return names;
        }
        for (JsonNode variable : variables) {
            if (variable.isObject() && variable.has("name")) {
                String name = variable.path("name").asText("").trim();
                if (!name.isBlank()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private void collectOutputVariable(JsonNode config, Set<String> outputVariables) {
        if (config == null || !config.isObject()) {
            return;
        }
        String outputVariable = config.path("outputVariable").asText("").trim();
        if (!outputVariable.isBlank()) {
            outputVariables.add(outputVariable);
        }
        if (config.has("variable") && config.path("variable").isTextual()) {
            outputVariables.add(config.path("variable").asText().trim());
        }
    }

    private void validateTemplateReferences(String nodeId,
                                            JsonNode config,
                                            Set<String> declaredVariables,
                                            Set<String> outputVariables,
                                            List<String> errors) {
        if (config == null) {
            return;
        }
        String configText = config.toString();
        Matcher matcher = TEMPLATE_VAR_PATTERN.matcher(configText);
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            String root = expression.contains(".") ? expression.substring(0, expression.indexOf('.')) : expression;
            if ("input".equals(root) || declaredVariables.contains(root) || outputVariables.contains(root)) {
                continue;
            }
            if (isKnownBuiltin(root)) {
                continue;
            }
            errors.add("节点 " + nodeId + " 引用了未声明变量: " + expression);
        }
    }

    private boolean isKnownBuiltin(String root) {
        return Set.of(
                "input", "output", "conditionResult", "switchResult", "loopItem",
                "loopIndex", "loopResults", "parallelResults", "codeResult", "agentResult",
                "toolResult", "subWorkflowResult"
        ).contains(root);
    }

    private void validateNodeConfig(String nodeId,
                                    String type,
                                    JsonNode config,
                                    Long workspaceId,
                                    List<String> errors) {
        if ("Tool".equalsIgnoreCase(type)) {
            validateToolNode(nodeId, config, workspaceId, errors);
        }
        if ("Webhook".equalsIgnoreCase(type)) {
            validateWebhookNode(nodeId, config, errors);
        }
        if ("SubWorkflow".equalsIgnoreCase(type)) {
            validateSubWorkflowNode(nodeId, config, workspaceId, errors);
        }
        if ("Agent".equalsIgnoreCase(type)) {
            validateAgentNode(nodeId, config, workspaceId, errors);
        }
        if ("Knowledge".equalsIgnoreCase(type)) {
            validateKnowledgeNode(nodeId, config, workspaceId, errors);
        }
        if ("LLM".equalsIgnoreCase(type)) {
            validateLlmNode(nodeId, config, errors);
        }
        if ("Switch".equalsIgnoreCase(type)) {
            validateSwitchNode(nodeId, config, errors);
        }
        if ("Condition".equalsIgnoreCase(type)) {
            validateConditionNode(nodeId, config, errors);
        }
    }

    private void validateAgentNode(String nodeId, JsonNode config, Long workspaceId, List<String> errors) {
        if (config == null || !config.isObject()) {
            errors.add("Agent 节点 " + nodeId + " 缺少 config");
            return;
        }
        if (!config.has("agentId")) {
            errors.add("Agent 节点 " + nodeId + " 缺少 agentId");
            return;
        }
        if (workspaceId == null) {
            return;
        }
        long agentId = config.path("agentId").asLong();
        Agent agent = agentRepository.findById(agentId).orElse(null);
        if (agent == null || !workspaceId.equals(agent.getWorkspaceId())) {
            errors.add("Agent 节点 " + nodeId + " 引用的智能体不存在: " + agentId);
        }
    }

    private void validateKnowledgeNode(String nodeId, JsonNode config, Long workspaceId, List<String> errors) {
        if (config == null || !config.isObject()) {
            errors.add("Knowledge 节点 " + nodeId + " 缺少 config");
            return;
        }
        if (!config.has("knowledgeBaseId")) {
            errors.add("Knowledge 节点 " + nodeId + " 缺少 knowledgeBaseId");
            return;
        }
        if (workspaceId == null) {
            return;
        }
        long knowledgeBaseId = config.path("knowledgeBaseId").asLong();
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId).orElse(null);
        if (knowledgeBase == null || !workspaceId.equals(knowledgeBase.getWorkspaceId())) {
            errors.add("Knowledge 节点 " + nodeId + " 引用的知识库不存在: " + knowledgeBaseId);
        }
    }

    private void validateLlmNode(String nodeId, JsonNode config, List<String> errors) {
        if (config == null || !config.isObject()) {
            errors.add("LLM 节点 " + nodeId + " 缺少 config");
            return;
        }
        if (!config.has("platformModelId")) {
            errors.add("LLM 节点 " + nodeId + " 缺少 platformModelId");
            return;
        }
        long platformModelId = config.path("platformModelId").asLong();
        PlatformModel model = platformModelRepository.findById(platformModelId).orElse(null);
        if (model == null || model.getStatus() == null || model.getStatus() != 1) {
            errors.add("LLM 节点 " + nodeId + " 引用的模型不存在或已禁用: " + platformModelId);
        }
    }

    private void validateSubWorkflowNode(String nodeId, JsonNode config, Long workspaceId, List<String> errors) {
        if (config == null || !config.isObject()) {
            errors.add("Sub Workflow 节点 " + nodeId + " 缺少 config");
            return;
        }
        if (!config.has("workflowId")) {
            errors.add("Sub Workflow 节点 " + nodeId + " 缺少 workflowId");
            return;
        }
        if (workspaceId == null) {
            return;
        }
        long workflowId = config.path("workflowId").asLong();
        Workflow workflow = workflowRepository.findById(workflowId).orElse(null);
        if (workflow == null || !workspaceId.equals(workflow.getWorkspaceId())) {
            errors.add("Sub Workflow 节点 " + nodeId + " 引用的工作流不存在: " + workflowId);
        }
    }

    private void validateWebhookNode(String nodeId, JsonNode config, List<String> errors) {
        if (config == null || !config.isObject()) {
            errors.add("Webhook 节点 " + nodeId + " 缺少 config");
            return;
        }
        if (!config.has("url") || config.path("url").asText("").isBlank()) {
            errors.add("Webhook 节点 " + nodeId + " 缺少 url");
        }
    }

    private void validateToolNode(String nodeId, JsonNode config, Long workspaceId, List<String> errors) {
        if (config == null || !config.isObject()) {
            errors.add("Tool 节点 " + nodeId + " 缺少 config");
            return;
        }
        String sourceType = config.path("sourceType").asText("HTTP").trim().toUpperCase();
        if ("MCP".equals(sourceType)) {
            if (!config.has("mcpServerId")) {
                errors.add("Tool 节点 " + nodeId + " 缺少 mcpServerId");
            } else if (workspaceId != null) {
                long mcpServerId = config.path("mcpServerId").asLong();
                McpServer server = mcpServerRepository.findById(mcpServerId).orElse(null);
                if (server == null || !workspaceId.equals(server.getWorkspaceId())) {
                    errors.add("Tool 节点 " + nodeId + " 引用的 MCP 服务不存在: " + mcpServerId);
                }
            }
            if (!config.has("mcpToolName") || config.path("mcpToolName").asText("").isBlank()) {
                errors.add("Tool 节点 " + nodeId + " 缺少 mcpToolName");
            }
            return;
        }
        if (!config.has("toolId")) {
            errors.add("Tool 节点 " + nodeId + " 缺少 toolId");
            return;
        }
        if (workspaceId == null) {
            return;
        }
        long toolId = config.path("toolId").asLong();
        Tool tool = toolRepository.findById(toolId).orElse(null);
        if (tool == null || !workspaceId.equals(tool.getWorkspaceId())) {
            errors.add("Tool 节点 " + nodeId + " 引用的工具不存在: " + toolId);
        }
    }

    private void validateConditionNode(String nodeId, JsonNode config, List<String> errors) {
        if (config == null || !config.isObject()) {
            errors.add("Condition 节点 " + nodeId + " 缺少 config");
            return;
        }
        if (!config.has("variable") || config.path("variable").asText("").isBlank()) {
            errors.add("Condition 节点 " + nodeId + " 缺少 variable");
        }
    }

    private void validateSwitchNode(String nodeId, JsonNode config, List<String> errors) {
        if (config == null || !config.isObject()) {
            errors.add("Switch 节点 " + nodeId + " 缺少 config");
            return;
        }
        if (!config.has("variable") || config.path("variable").asText("").isBlank()) {
            errors.add("Switch 节点 " + nodeId + " 缺少 variable");
        }
        JsonNode cases = config.get("cases");
        if (cases == null || !cases.isArray() || cases.isEmpty()) {
            errors.add("Switch 节点 " + nodeId + " 至少需要一个 case");
        } else {
            for (JsonNode caseNode : cases) {
                if (!caseNode.isObject() || caseNode.path("id").asText("").isBlank()) {
                    errors.add("Switch 节点 " + nodeId + " 存在缺少 id 的 case");
                }
            }
        }
    }

    private void validateBranchNode(String nodeId,
                                      String type,
                                      JsonNode config,
                                      Set<String> handles,
                                      List<String> errors) {
        if ("Condition".equalsIgnoreCase(type)) {
            if (!handles.contains("true")) {
                errors.add("Condition 节点 " + nodeId + " 缺少 true 分支连线");
            }
            if (!handles.contains("false")) {
                errors.add("Condition 节点 " + nodeId + " 缺少 false 分支连线");
            }
            return;
        }
        if (!"Switch".equalsIgnoreCase(type)) {
            return;
        }
        if (config == null || !config.isObject()) {
            return;
        }
        JsonNode cases = config.get("cases");
        if (cases != null && cases.isArray()) {
            for (JsonNode caseNode : cases) {
                String caseId = caseNode.path("id").asText("");
                if (!caseId.isBlank() && !handles.contains(caseId)) {
                    errors.add("Switch 节点 " + nodeId + " 缺少 case 分支连线: " + caseId);
                }
            }
        }
        String defaultCase = config.path("defaultCase").asText("default");
        if (!handles.contains(defaultCase)) {
            errors.add("Switch 节点 " + nodeId + " 缺少默认分支连线: " + defaultCase);
        }
    }

    private boolean hasCycle(Map<String, List<String>> adjacency, Set<String> nodeIds) {
        Map<String, Integer> state = new HashMap<>();
        for (String nodeId : nodeIds) {
            if (state.getOrDefault(nodeId, 0) == 0 && dfsCycle(nodeId, adjacency, state)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfsCycle(String node, Map<String, List<String>> adjacency, Map<String, Integer> state) {
        int current = state.getOrDefault(node, 0);
        if (current == 1) {
            return true;
        }
        if (current == 2) {
            return false;
        }
        state.put(node, 1);
        for (String next : adjacency.getOrDefault(node, List.of())) {
            if (dfsCycle(next, adjacency, state)) {
                return true;
            }
        }
        state.put(node, 2);
        return false;
    }
}
