package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.platform.PlatformCredential;
import com.boxai.domain.platform.PlatformCredentialRepository;
import com.boxai.infrastructure.persistence.entity.PlatformCredentialDO;
import com.boxai.infrastructure.persistence.mapper.PlatformCredentialMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class PlatformCredentialRepositoryImpl implements PlatformCredentialRepository {

    private final PlatformCredentialMapper mapper;

    public PlatformCredentialRepositoryImpl(PlatformCredentialMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PlatformCredential save(PlatformCredential credential) {
        PlatformCredentialDO row = toDo(credential);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        credential.setId(row.getId());
        credential.setCreatedAt(row.getCreatedAt());
        credential.setUpdatedAt(row.getUpdatedAt());
        return credential;
    }

    @Override
    public void update(PlatformCredential credential) {
        PlatformCredentialDO row = toDo(credential);
        row.setId(credential.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        credential.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public Optional<PlatformCredential> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<PlatformCredential> findActiveByProvider(Long providerId) {
        return Optional.ofNullable(mapper.selectOneByQuery(
                        QueryWrapper.create().eq("provider_id", providerId).eq("status", 1)
                                .orderBy("id", false).limit(1)))
                .map(this::toDomain);
    }

    @Override
    public Set<Long> listActiveProviderIds() {
        return mapper.selectListByQuery(QueryWrapper.create().eq("status", 1))
                .stream()
                .map(PlatformCredentialDO::getProviderId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    @Override
    public List<PlatformCredential> listByProvider(Long providerId) {
        return mapper.selectListByQuery(QueryWrapper.create().eq("provider_id", providerId).orderBy("id", false))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void touchLastUsed(Long id) {
        PlatformCredentialDO patch = new PlatformCredentialDO();
        patch.setId(id);
        patch.setLastUsedAt(LocalDateTime.now());
        mapper.update(patch);
    }

    private PlatformCredential toDomain(PlatformCredentialDO row) {
        PlatformCredential credential = new PlatformCredential();
        credential.setId(row.getId());
        credential.setProviderId(row.getProviderId());
        credential.setCredentialName(row.getCredentialName());
        credential.setEncryptedApiKey(row.getEncryptedApiKey());
        credential.setStatus(row.getStatus());
        credential.setLastUsedAt(row.getLastUsedAt());
        credential.setCreatedAt(row.getCreatedAt());
        credential.setUpdatedAt(row.getUpdatedAt());
        return credential;
    }

    private PlatformCredentialDO toDo(PlatformCredential credential) {
        PlatformCredentialDO row = new PlatformCredentialDO();
        row.setProviderId(credential.getProviderId());
        row.setCredentialName(credential.getCredentialName());
        row.setEncryptedApiKey(credential.getEncryptedApiKey());
        row.setStatus(credential.getStatus() == null ? 1 : credential.getStatus());
        return row;
    }
}
