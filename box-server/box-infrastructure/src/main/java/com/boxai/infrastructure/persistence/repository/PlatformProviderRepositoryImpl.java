package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.platform.PlatformProvider;
import com.boxai.domain.platform.PlatformProviderRepository;
import com.boxai.infrastructure.persistence.entity.PlatformProviderDO;
import com.boxai.infrastructure.persistence.mapper.PlatformProviderMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PlatformProviderRepositoryImpl implements PlatformProviderRepository {

    private final PlatformProviderMapper mapper;

    public PlatformProviderRepositoryImpl(PlatformProviderMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PlatformProvider save(PlatformProvider provider) {
        PlatformProviderDO row = toDo(provider);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        provider.setId(row.getId());
        provider.setCreatedAt(row.getCreatedAt());
        provider.setUpdatedAt(row.getUpdatedAt());
        return provider;
    }

    @Override
    public void update(PlatformProvider provider) {
        PlatformProviderDO row = toDo(provider);
        row.setId(provider.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        provider.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public Optional<PlatformProvider> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<PlatformProvider> listAll() {
        return mapper.selectListByQuery(QueryWrapper.create().orderBy("id", true))
                .stream().map(this::toDomain).toList();
    }

    private PlatformProvider toDomain(PlatformProviderDO row) {
        PlatformProvider provider = new PlatformProvider();
        provider.setId(row.getId());
        provider.setProviderCode(row.getProviderCode());
        provider.setProviderName(row.getProviderName());
        provider.setProviderType(row.getProviderType());
        provider.setBaseUrl(row.getBaseUrl());
        provider.setStatus(row.getStatus());
        provider.setCreatedAt(row.getCreatedAt());
        provider.setUpdatedAt(row.getUpdatedAt());
        return provider;
    }

    private PlatformProviderDO toDo(PlatformProvider provider) {
        PlatformProviderDO row = new PlatformProviderDO();
        row.setProviderCode(provider.getProviderCode());
        row.setProviderName(provider.getProviderName());
        row.setProviderType(provider.getProviderType());
        row.setBaseUrl(provider.getBaseUrl());
        row.setStatus(provider.getStatus() == null ? 1 : provider.getStatus());
        return row;
    }
}
