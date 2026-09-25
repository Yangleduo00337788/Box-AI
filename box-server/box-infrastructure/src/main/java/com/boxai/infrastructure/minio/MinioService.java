package com.boxai.infrastructure.minio;

import com.boxai.domain.storage.ObjectStorage;
import org.springframework.stereotype.Service;

@Service
public class MinioService {

    private final ObjectStorage objectStorage;

    public MinioService(ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    public boolean ping() {
        return objectStorage.ping(null);
    }
}
