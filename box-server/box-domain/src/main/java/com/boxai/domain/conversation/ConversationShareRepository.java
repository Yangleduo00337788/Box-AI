package com.boxai.domain.conversation;

import java.util.Optional;

public interface ConversationShareRepository {

    ConversationShare save(ConversationShare share);

    Optional<ConversationShare> findByToken(String token);
}
