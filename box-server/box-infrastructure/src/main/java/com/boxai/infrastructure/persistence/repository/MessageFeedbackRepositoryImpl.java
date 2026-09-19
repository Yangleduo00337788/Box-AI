package com.boxai.infrastructure.persistence.repository;

import com.boxai.common.result.PageResult;
import com.boxai.domain.conversation.MessageFeedback;
import com.boxai.domain.conversation.MessageFeedbackQuery;
import com.boxai.domain.conversation.MessageFeedbackRepository;
import com.boxai.infrastructure.persistence.entity.MessageFeedbackDO;
import com.boxai.infrastructure.persistence.mapper.MessageFeedbackMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        row.setStatus(feedback.getStatus());
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        feedback.setId(row.getId());
        feedback.setCreatedAt(row.getCreatedAt());
        return feedback;
    }

    @Override
    public Optional<MessageFeedback> findById(Long id) {
        MessageFeedbackDO row = mapper.selectOneById(id);
        return row == null ? Optional.empty() : Optional.of(toDomain(row));
    }

    @Override
    public PageResult<MessageFeedback> page(MessageFeedbackQuery query) {
        int page = Math.max(query.getPage(), 1);
        int pageSize = Math.min(Math.max(query.getPageSize(), 1), 100);
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("rating", query.getRating(), query.getRating() != null && !query.getRating().isBlank())
                .eq("status", query.getStatus(), query.getStatus() != null && !query.getStatus().isBlank())
                .orderBy("created_at", false);
        Page<MessageFeedbackDO> result = mapper.paginate(page, pageSize, wrapper);
        List<MessageFeedback> records = result.getRecords().stream().map(this::toDomain).toList();
        return new PageResult<>(records, result.getTotalRow(), page, pageSize);
    }

    @Override
    public long countByRatingAndStatus(String rating, String status) {
        return mapper.selectCountByQuery(QueryWrapper.create()
                .eq("rating", rating)
                .eq("status", status));
    }

    @Override
    public void updateReply(MessageFeedback feedback) {
        MessageFeedbackDO row = new MessageFeedbackDO();
        row.setId(feedback.getId());
        row.setStatus(feedback.getStatus());
        row.setAdminReply(feedback.getAdminReply());
        row.setAdminReplyBy(feedback.getAdminReplyBy());
        row.setAdminRepliedAt(feedback.getAdminRepliedAt());
        mapper.update(row);
    }

    private MessageFeedback toDomain(MessageFeedbackDO row) {
        MessageFeedback feedback = new MessageFeedback();
        feedback.setId(row.getId());
        feedback.setWorkspaceId(row.getWorkspaceId());
        feedback.setConversationId(row.getConversationId());
        feedback.setMessageId(row.getMessageId());
        feedback.setUserId(row.getUserId());
        feedback.setRating(row.getRating());
        feedback.setContent(row.getContent());
        feedback.setStatus(row.getStatus());
        feedback.setAdminReply(row.getAdminReply());
        feedback.setAdminReplyBy(row.getAdminReplyBy());
        feedback.setAdminRepliedAt(row.getAdminRepliedAt());
        feedback.setCreatedAt(row.getCreatedAt());
        return feedback;
    }
}
