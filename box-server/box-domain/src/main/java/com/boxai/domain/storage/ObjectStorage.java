package com.boxai.domain.storage;

import java.io.InputStream;

public interface ObjectStorage {

    /** 当前用于新上传的后端（MINIO / R2）。 */
    String activeBackend();

    String defaultBucket();

    void put(String bucket, String key, InputStream data, long size, String contentType);

    default InputStream get(String bucket, String key) {
        return get(null, bucket, key);
    }

    /** backend 为空时按历史 MinIO 读取。 */
    InputStream get(String backend, String bucket, String key);

    default void delete(String bucket, String key) {
        delete(null, bucket, key);
    }

    void delete(String backend, String bucket, String key);

    /** backend 为空时检测当前激活后端。 */
    boolean ping(String backend);
}
