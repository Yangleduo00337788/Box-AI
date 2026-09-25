package com.boxai.infrastructure.storage;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.net.URI;

/**
 * Cloudflare R2：使用 AWS SDK（path-style + endpointOverride），避免 MinIO SDK Put 出现 Access Denied。
 */
public class AwsS3R2ObjectStorage implements ObjectStorageBackend {

    private final S3Client client;
    private final String defaultBucket;

    public AwsS3R2ObjectStorage(String endpoint, String accessKey, String secretKey, String defaultBucket) {
        this.defaultBucket = defaultBucket;
        // AWS SDK 2.25+ 默认对 Put 加校验和，R2 可能因此返回 403 Access Denied
        System.setProperty("aws.requestChecksumCalculation", "when_required");
        System.setProperty("aws.responseChecksumValidation", "when_required");
        this.client = S3Client.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    @Override
    public String defaultBucket() {
        return defaultBucket;
    }

    @Override
    public void put(String bucket, String key, InputStream data, long size, String contentType) {
        try {
            PutObjectRequest.Builder request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key);
            if (contentType != null && !contentType.isBlank()) {
                request.contentType(contentType);
            }
            client.putObject(request.build(), RequestBody.fromInputStream(data, size));
        } catch (S3Exception e) {
            throw storageException("文件上传失败", e);
        } catch (Exception e) {
            throw new IllegalStateException("文件上传失败", e);
        }
    }

    @Override
    public InputStream get(String bucket, String key) {
        try {
            return client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw storageException("文件读取失败", e);
        } catch (Exception e) {
            throw new IllegalStateException("文件读取失败", e);
        }
    }

    @Override
    public void delete(String bucket, String key) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw storageException("文件删除失败", e);
        } catch (Exception e) {
            throw new IllegalStateException("文件删除失败", e);
        }
    }

    @Override
    public boolean ping() {
        String probeKey = ".box-storage-probe/" + System.currentTimeMillis();
        try {
            byte[] payload = "ok".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            client.putObject(
                    PutObjectRequest.builder().bucket(defaultBucket).key(probeKey).build(),
                    RequestBody.fromBytes(payload));
            client.deleteObject(DeleteObjectRequest.builder().bucket(defaultBucket).key(probeKey).build());
            return true;
        } catch (Exception e) {
            try {
                client.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(defaultBucket)
                        .maxKeys(1)
                        .build());
                return false;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private static IllegalStateException storageException(String message, S3Exception e) {
        String detail = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = e.statusCode() + "";
        }
        return new IllegalStateException(message + ": " + detail, e);
    }
}
