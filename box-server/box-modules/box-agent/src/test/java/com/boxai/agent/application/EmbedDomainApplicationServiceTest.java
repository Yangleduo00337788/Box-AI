package com.boxai.agent.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.publish.EmbedCustomDomain;
import com.boxai.domain.publish.EmbedCustomDomainRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmbedDomainApplicationServiceTest {

    private final EmbedCustomDomainRepository embedCustomDomainRepository = mock(EmbedCustomDomainRepository.class);
    private final AgentRepository agentRepository = mock(AgentRepository.class);
    private final EmbedDomainApplicationService service = new EmbedDomainApplicationService(
            embedCustomDomainRepository, agentRepository, true, "", "platform");

    @Test
    void findVerifiedByHostIgnoresUnverifiedRecords() {
        EmbedCustomDomain domain = new EmbedCustomDomain();
        domain.setDomain("chat.example.com");
        domain.setVerified(0);
        when(embedCustomDomainRepository.findByDomain("chat.example.com")).thenReturn(Optional.of(domain));

        assertTrue(service.findVerifiedByHost("https://chat.example.com").isEmpty());
    }

    @Test
    void findVerifiedByHostReturnsVerifiedDomain() {
        EmbedCustomDomain domain = new EmbedCustomDomain();
        domain.setAgentId(9L);
        domain.setDomain("chat.example.com");
        domain.setVerified(1);
        when(embedCustomDomainRepository.findByDomain("chat.example.com")).thenReturn(Optional.of(domain));

        assertEquals(9L, service.findVerifiedByHost("chat.example.com").orElseThrow().getAgentId());
    }

    @Test
    void syncDomainRejectsDomainOwnedByAnotherAgent() {
        Agent agent = new Agent();
        agent.setId(1L);
        agent.setWorkspaceId(7L);
        EmbedCustomDomain occupied = new EmbedCustomDomain();
        occupied.setAgentId(2L);
        occupied.setDomain("chat.example.com");
        when(embedCustomDomainRepository.findByDomain("chat.example.com")).thenReturn(Optional.of(occupied));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.syncDomain(agent, "chat.example.com"));
        assertEquals(ErrorCode.CONFLICT, ex.getCode());
    }

    @Test
    void syncDomainClearsBindingWhenBlank() {
        Agent agent = new Agent();
        agent.setId(1L);

        assertEquals(null, service.syncDomain(agent, "  "));
        verify(embedCustomDomainRepository).deleteByAgentId(1L);
    }
}
