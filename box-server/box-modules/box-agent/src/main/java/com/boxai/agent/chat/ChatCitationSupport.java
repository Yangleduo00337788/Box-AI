package com.boxai.agent.chat;

import com.boxai.knowledge.api.KnowledgeCitationVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public final class ChatCitationSupport {

    private ChatCitationSupport() {
    }

    public static String toCitationsJson(ObjectMapper objectMapper, List<KnowledgeCitationVO> citations) {
        if (citations == null || citations.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static String toCitationsJson(List<KnowledgeCitationVO> citations) {
        return toCitationsJson(new ObjectMapper(), citations);
    }
}
