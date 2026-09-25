package com.boxai.infrastructure.storage;

import com.boxai.domain.storage.ObjectStorage;
import com.boxai.domain.storage.StorageBackendKind;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@Primary
public class RoutingObjectStorage implements ObjectStorage {

    private final ObjectStorageRuntime runtime;

    public RoutingObjectStorage(ObjectStorageRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public String activeBackend() {
        return runtime.activeKind().name();
    }

    @Override
    public String defaultBucket() {
        return runtime.activeBucket();
    }

    @Override
    public void put(String bucket, String key, InputStream data, long size, String contentType) {
        runtime.delegate(runtime.activeKind()).put(bucket, key, data, size, contentType);
    }

    @Override
    public InputStream get(String backend, String bucket, String key) {
        StorageBackendKind kind = runtime.resolveReadBackend(backend);
        return runtime.delegate(kind).get(bucket, key);
    }

    @Override
    public void delete(String backend, String bucket, String key) {
        StorageBackendKind kind = runtime.resolveReadBackend(backend);
        runtime.delegate(kind).delete(bucket, key);
    }

    @Override
    public boolean ping(String backend) {
        StorageBackendKind kind = backend == null || backend.isBlank()
                ? runtime.activeKind()
                : StorageBackendKind.parse(backend);
        return runtime.ping(kind);
    }
}
