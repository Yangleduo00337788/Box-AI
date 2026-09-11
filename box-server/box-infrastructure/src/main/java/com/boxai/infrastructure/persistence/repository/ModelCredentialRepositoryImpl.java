package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.model.ModelCredential;
import com.boxai.domain.model.ModelCredentialRepository;
import com.boxai.infrastructure.persistence.entity.ModelCredentialDO;
import com.boxai.infrastructure.persistence.mapper.ModelCredentialMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ModelCredentialRepositoryImpl implements ModelCredentialRepository {

    private final ModelCredentialMapper mapper;

    public ModelCredentialRepositoryImpl(ModelCredentialMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ModelCredential save(ModelCredential credential) {
        ModelCredentialDO row = new ModelCredentialDO();
        row.setWorkspaceId(credential.getWorkspaceId());
        row.setProviderId(credential.getProviderId());
        row.setCredentialName(credential.getCredentialName());
        row.setEncryptedApiKey(credential.getEncryptedApiKey());
        row.setStatus(credential.getStatus() == null ? 1 : credential.getStatus());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        credential.setId(row.getId());
        return credential;
    }

    @Override
    public Optional<ModelCredential> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<ModelCredential> findActiveByProvider(Long workspaceId, Long providerId) {
        ModelCredentialDO row = mapper.selectOneByQuery(QueryWrapper.create()
                .eq("workspace_id", workspaceId)
                .eq("provider_id", providerId)
                .eq("status", 1)
                .orderBy("id", false)
                .limit(1));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public List<ModelCredential> listByWorkspace(Long workspaceId) {
        return mapper.selectListByQuery(QueryWrapper.create().eq("workspace_id", workspaceId).orderBy("id", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void touchLastUsed(Long id) {
        ModelCredentialDO patch = new ModelCredentialDO();
        patch.setId(id);
        patch.setLastUsedAt(LocalDateTime.now());
        mapper.update(patch);
    }

    private ModelCredential toDomain(ModelCredentialDO row) {
        ModelCredential credential = new ModelCredential();
        credential.setId(row.getId());
        credential.setWorkspaceId(row.getWorkspaceId());
        credential.setProviderId(row.getProviderId());
        credential.setCredentialName(row.getCredentialName());
        credential.setEncryptedApiKey(row.getEncryptedApiKey());
        credential.setStatus(row.getStatus());
        return credential;
    }
}
