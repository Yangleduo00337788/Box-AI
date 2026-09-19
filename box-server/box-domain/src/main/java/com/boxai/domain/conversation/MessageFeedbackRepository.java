package com.boxai.domain.conversation;

import com.boxai.common.result.PageResult;

import java.util.Optional;

public interface MessageFeedbackRepository {

    MessageFeedback save(MessageFeedback feedback);

    Optional<MessageFeedback> findById(Long id);

    PageResult<MessageFeedback> page(MessageFeedbackQuery query);

    void updateReply(MessageFeedback feedback);

    long countByRatingAndStatus(String rating, String status);
}
