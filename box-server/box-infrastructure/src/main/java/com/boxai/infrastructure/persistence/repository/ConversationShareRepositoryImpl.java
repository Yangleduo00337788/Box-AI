package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.conversation.ConversationShare;
import com.boxai.domain.conversation.ConversationShareRepository;
import com.boxai.infrastructure.persistence.entity.ConversationShareDO;
import com.boxai.infrastructure.persistence.mapper.ConversationShareMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class ConversationShareRepositoryImpl implements ConversationShareRepository {

    private final ConversationShareMapper mapper;

    public ConversationShareRepositoryImpl(ConversationShareMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ConversationShare save(ConversationShare share) {
        ConversationShareDO row = toDo(share);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        share.setId(row.getId());
        share.setCreatedAt(row.getCreatedAt());
        return share;
    }

    @Override
    public Optional<ConversationShare> findByToken(String token) {
        ConversationShareDO row = mapper.selectOneByQuery(
                QueryWrapper.create().eq("token", token));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    private ConversationShareDO toDo(ConversationShare share) {
        ConversationShareDO row = new ConversationShareDO();
        row.setId(share.getId());
        row.setToken(share.getToken());
        row.setWorkspaceId(share.getWorkspaceId());
        row.setConversationId(share.getConversationId());
        row.setTitle(share.getTitle());
        row.setPayloadJson(share.getPayloadJson());
        row.setCreatedBy(share.getCreatedBy());
        row.setCreatedAt(share.getCreatedAt());
        return row;
    }

    private ConversationShare toDomain(ConversationShareDO row) {
        ConversationShare share = new ConversationShare();
        share.setId(row.getId());
        share.setToken(row.getToken());
        share.setWorkspaceId(row.getWorkspaceId());
        share.setConversationId(row.getConversationId());
        share.setTitle(row.getTitle());
        share.setPayloadJson(row.getPayloadJson());
        share.setCreatedBy(row.getCreatedBy());
        share.setCreatedAt(row.getCreatedAt());
        return share;
    }
}
