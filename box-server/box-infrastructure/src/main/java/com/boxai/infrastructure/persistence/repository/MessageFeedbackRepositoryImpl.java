package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.conversation.MessageFeedback;
import com.boxai.domain.conversation.MessageFeedbackRepository;
import com.boxai.infrastructure.persistence.entity.MessageFeedbackDO;
import com.boxai.infrastructure.persistence.mapper.MessageFeedbackMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class MessageFeedbackRepositoryImpl implements MessageFeedbackRepository {

    private final MessageFeedbackMapper mapper;

    public MessageFeedbackRepositoryImpl(MessageFeedbackMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public MessageFeedback save(MessageFeedback feedback) {
        MessageFeedbackDO row = new MessageFeedbackDO();
        row.setWorkspaceId(feedback.getWorkspaceId());
        row.setConversationId(feedback.getConversationId());
        row.setMessageId(feedback.getMessageId());
        row.setUserId(feedback.getUserId());
        row.setRating(feedback.getRating());
        row.setContent(feedback.getContent());
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        feedback.setId(row.getId());
        feedback.setCreatedAt(row.getCreatedAt());
        return feedback;
    }
}
