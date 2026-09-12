package com.boxai.security.guard;

import com.boxai.common.exception.DependencyConflictException;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentKnowledgeRepository;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentSubAgentRepository;
import com.boxai.domain.agent.AgentToolRepository;
import com.boxai.domain.publish.PublishRepository;
import com.boxai.domain.workflow.WorkflowVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceDeleteGuardTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private AgentKnowledgeRepository agentKnowledgeRepository;
    @Mock
    private AgentToolRepository agentToolRepository;
    @Mock
    private AgentSubAgentRepository agentSubAgentRepository;
    @Mock
    private PublishRepository publishRepository;
    @Mock
    private WorkflowVersionRepository workflowVersionRepository;

    @InjectMocks
    private ResourceDeleteGuard resourceDeleteGuard;

    @Test
    void publishedAgentCannotBeDeleted() {
        Agent agent = new Agent();
        agent.setId(1L);
        agent.setName("Demo");
        agent.setStatus("PUBLISHED");
        agent.setPublishedVersionId(10L);
        when(agentSubAgentRepository.listParentAgentIdsBySubAgentId(1L)).thenReturn(List.of());
        when(publishRepository.findLatestActive("AGENT", 1L)).thenReturn(Optional.empty());

        assertThrows(DependencyConflictException.class, () -> resourceDeleteGuard.assertAgentDeletable(agent));
    }

    @Test
    void draftAgentWithoutDependenciesCanBeDeleted() {
        Agent agent = new Agent();
        agent.setId(2L);
        agent.setName("Draft");
        agent.setStatus("DRAFT");
        when(agentSubAgentRepository.listParentAgentIdsBySubAgentId(2L)).thenReturn(List.of());
        when(publishRepository.findLatestActive("AGENT", 2L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> resourceDeleteGuard.assertAgentDeletable(agent));
    }
}
