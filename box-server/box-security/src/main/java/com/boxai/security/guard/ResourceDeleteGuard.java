package com.boxai.security.guard;

import com.boxai.common.constant.PublishResourceTypes;
import com.boxai.common.exception.DependencyConflictException;
import com.boxai.common.guard.ResourceDependency;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentKnowledgeRepository;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentSubAgentRepository;
import com.boxai.domain.agent.AgentToolRepository;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.publish.PublishRepository;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowVersionRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ResourceDeleteGuard {

    private final AgentRepository agentRepository;
    private final AgentKnowledgeRepository agentKnowledgeRepository;
    private final AgentToolRepository agentToolRepository;
    private final AgentSubAgentRepository agentSubAgentRepository;
    private final PublishRepository publishRepository;
    private final WorkflowVersionRepository workflowVersionRepository;

    public ResourceDeleteGuard(AgentRepository agentRepository,
                               AgentKnowledgeRepository agentKnowledgeRepository,
                               AgentToolRepository agentToolRepository,
                               AgentSubAgentRepository agentSubAgentRepository,
                               PublishRepository publishRepository,
                               WorkflowVersionRepository workflowVersionRepository) {
        this.agentRepository = agentRepository;
        this.agentKnowledgeRepository = agentKnowledgeRepository;
        this.agentToolRepository = agentToolRepository;
        this.agentSubAgentRepository = agentSubAgentRepository;
        this.publishRepository = publishRepository;
        this.workflowVersionRepository = workflowVersionRepository;
    }

    public void assertAgentDeletable(Agent agent) {
        List<ResourceDependency> dependencies = new ArrayList<>();
        if ("PUBLISHED".equalsIgnoreCase(agent.getStatus()) || agent.getPublishedVersionId() != null) {
            dependencies.add(new ResourceDependency("publish", agent.getId(), agent.getName(), "published"));
        }
        publishRepository.findLatestActive(PublishResourceTypes.AGENT, agent.getId())
                .ifPresent(publish -> dependencies.add(new ResourceDependency(
                        "publish",
                        publish.getId(),
                        agent.getName(),
                        "active_publish")));
        for (Long parentAgentId : agentSubAgentRepository.listParentAgentIdsBySubAgentId(agent.getId())) {
            agentRepository.findById(parentAgentId).ifPresent(parent -> dependencies.add(new ResourceDependency(
                    "agent",
                    parent.getId(),
                    parent.getName(),
                    "sub_agent_binding")));
        }
        throwIfNeeded("智能体存在依赖，无法删除", dependencies);
    }

    public void assertKnowledgeDeletable(KnowledgeBase knowledgeBase) {
        List<ResourceDependency> dependencies = agentDependencies(
                agentKnowledgeRepository.listDistinctAgentIdsByKnowledgeBaseId(knowledgeBase.getId()),
                "knowledge_binding");
        throwIfNeeded("知识库正在被智能体使用，无法删除", dependencies);
    }

    public void assertToolDeletable(Tool tool) {
        List<ResourceDependency> dependencies = agentDependencies(
                agentToolRepository.listDistinctAgentIdsByToolId(tool.getId()),
                "tool_binding");
        throwIfNeeded("工具正在被智能体使用，无法删除", dependencies);
    }

    public void assertWorkflowDeletable(Workflow workflow) {
        List<ResourceDependency> dependencies = new ArrayList<>();
        if ("PUBLISHED".equalsIgnoreCase(workflow.getStatus()) || workflow.getPublishedVersionId() != null) {
            dependencies.add(new ResourceDependency("publish", workflow.getId(), workflow.getName(), "published"));
        }
        publishRepository.findLatestActive(PublishResourceTypes.WORKFLOW, workflow.getId())
                .ifPresent(publish -> dependencies.add(new ResourceDependency(
                        "publish",
                        publish.getId(),
                        workflow.getName(),
                        "active_publish")));
        int referenceCount = workflowVersionRepository.countSubWorkflowReferences(
                workflow.getId(),
                workflow.getWorkspaceId());
        if (referenceCount > 0) {
            dependencies.add(new ResourceDependency(
                    "workflow",
                    workflow.getId(),
                    workflow.getName(),
                    "sub_workflow_reference:" + referenceCount));
        }
        throwIfNeeded("工作流存在依赖，无法删除", dependencies);
    }

    private List<ResourceDependency> agentDependencies(List<Long> agentIds, String relation) {
        List<ResourceDependency> dependencies = new ArrayList<>();
        for (Long agentId : agentIds) {
            agentRepository.findById(agentId).ifPresent(agent -> dependencies.add(new ResourceDependency(
                    "agent",
                    agent.getId(),
                    agent.getName(),
                    relation)));
        }
        return dependencies;
    }

    private void throwIfNeeded(String message, List<ResourceDependency> dependencies) {
        if (!dependencies.isEmpty()) {
            throw new DependencyConflictException(message, dependencies);
        }
    }
}
