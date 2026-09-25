package com.boxai.infrastructure.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

import java.io.InputStream;

/** 自建 MinIO 等使用 MinIO Java SDK。 */
public class MinioObjectStorageBackend implements ObjectStorageBackend {

    private final MinioClient client;
    private final String defaultBucket;

    public MinioObjectStorageBackend(MinioClient client, String defaultBucket) {
        this.client = client;
        this.defaultBucket = defaultBucket;
    }

    @Override
    public String defaultBucket() {
        return defaultBucket;
    }

    @Override
    public void put(String bucket, String key, InputStream data, long size, String contentType) {
        try {
            ensureBucket(bucket);
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(data, size, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("文件上传失败", e);
        }
    }

    @Override
    public InputStream get(String bucket, String key) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new IllegalStateException("文件读取失败", e);
        }
    }

    @Override
    public void delete(String bucket, String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new IllegalStateException("文件删除失败", e);
        }
    }

    @Override
    public boolean ping() {
        try {
            ensureBucket(defaultBucket);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
