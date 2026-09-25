package com.boxai.infrastructure.storage;

import java.io.InputStream;

/** 单套 S3 兼容后端的读写实现（MinIO 或 R2）。 */
public interface ObjectStorageBackend {

    String defaultBucket();

    void put(String bucket, String key, InputStream data, long size, String contentType);

    InputStream get(String bucket, String key);

    void delete(String bucket, String key);

    boolean ping();
}
