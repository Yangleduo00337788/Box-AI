package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.conversation.Message;
import com.boxai.domain.conversation.MessageRepository;
import com.boxai.infrastructure.persistence.entity.MessageDO;
import com.boxai.infrastructure.persistence.mapper.MessageMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MessageRepositoryImpl implements MessageRepository {

    private final MessageMapper mapper;

    public MessageRepositoryImpl(MessageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Message save(Message message) {
        MessageDO row = toDo(message);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        message.setId(row.getId());
        message.setCreatedAt(row.getCreatedAt());
        return message;
    }

    @Override
    public List<Message> listByConversationId(Long conversationId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("conversation_id", conversationId)
                                .orderBy("sequence_no", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Message> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByConversationId(Long conversationId) {
        mapper.deleteByQuery(QueryWrapper.create().eq("conversation_id", conversationId));
    }

    @Override
    public Optional<Integer> findMaxSequenceNo(Long conversationId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("conversation_id", conversationId)
                                .orderBy("sequence_no", false)
                                .limit(1))
                .stream()
                .findFirst()
                .map(MessageDO::getSequenceNo);
    }

    private Message toDomain(MessageDO row) {
        Message message = new Message();
        message.setId(row.getId());
        message.setConversationId(row.getConversationId());
        message.setWorkspaceId(row.getWorkspaceId());
        message.setRole(row.getRole());
        message.setContent(row.getContent());
        message.setContentType(row.getContentType());
        message.setSequenceNo(row.getSequenceNo());
        message.setTokenCount(row.getTokenCount());
        message.setModelId(row.getModelId());
        message.setMetadataJson(row.getMetadata());
        message.setCreatedAt(row.getCreatedAt());
        return message;
    }

    private MessageDO toDo(Message message) {
        MessageDO row = new MessageDO();
        row.setConversationId(message.getConversationId());
        row.setWorkspaceId(message.getWorkspaceId());
        row.setRole(message.getRole());
        row.setContent(message.getContent());
        row.setContentType(message.getContentType() == null ? "TEXT" : message.getContentType());
        row.setSequenceNo(message.getSequenceNo());
        row.setTokenCount(message.getTokenCount());
        row.setModelId(message.getModelId());
        row.setMetadata(message.getMetadataJson());
        return row;
    }
}
