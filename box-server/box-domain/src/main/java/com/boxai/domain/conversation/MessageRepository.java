package com.boxai.domain.conversation;

import java.util.List;
import java.util.Optional;

public interface MessageRepository {

    Message save(Message message);

    List<Message> listByConversationId(Long conversationId);

    Optional<Message> findById(Long id);

    void deleteById(Long id);

    Optional<Integer> findMaxSequenceNo(Long conversationId);

    void deleteByConversationId(Long conversationId);
}
