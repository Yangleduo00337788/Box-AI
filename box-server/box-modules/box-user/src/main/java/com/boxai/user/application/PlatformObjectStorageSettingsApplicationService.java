package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.storage.StorageBackendKind;
import com.boxai.infrastructure.minio.MinioProperties;
import com.boxai.infrastructure.storage.ObjectStorageEndpointSupport;
import com.boxai.infrastructure.storage.ObjectStorageRuntime;
import com.boxai.infrastructure.storage.ObjectStorageSettingsDocument;
import com.boxai.user.api.platform.PlatformObjectStorageBackendVO;
import com.boxai.user.api.platform.PlatformObjectStorageSettingsVO;
import com.boxai.user.api.platform.TestPlatformObjectStorageRequest;
import com.boxai.user.api.platform.UpdatePlatformObjectStorageBackendRequest;
import com.boxai.user.api.platform.UpdatePlatformObjectStorageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformObjectStorageSettingsApplicationService {

    private final ObjectStorageRuntime objectStorageRuntime;
    private final MinioProperties yamlMinio;

    public PlatformObjectStorageSettingsApplicationService(ObjectStorageRuntime objectStorageRuntime,
                                                           MinioProperties yamlMinio) {
        this.objectStorageRuntime = objectStorageRuntime;
        this.yamlMinio = yamlMinio;
    }

    public PlatformObjectStorageSettingsVO getSettings() {
        ObjectStorageSettingsDocument doc = objectStorageRuntime.snapshotDocument();
        StorageBackendKind active = StorageBackendKind.parse(doc.getActive());
        return new PlatformObjectStorageSettingsVO(
                active.name(),
                objectStorageRuntime.activeBucket(),
                toBackendVo(StorageBackendKind.MINIO, doc.getMinio()),
                toBackendVo(StorageBackendKind.R2, doc.getR2()),
                buildHint(active));
    }

    @Transactional
    public PlatformObjectStorageSettingsVO update(UpdatePlatformObjectStorageRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求不能为空");
        }
        StorageBackendKind active = StorageBackendKind.parse(request.activeBackend());
        if (active != StorageBackendKind.MINIO && active != StorageBackendKind.R2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的存储后端");
        }
        ObjectStorageSettingsDocument doc = objectStorageRuntime.snapshotDocument();
        doc.setActive(active.name());
        mergeBackend(doc.getMinio(), request.minio(), true);
        mergeBackend(doc.getR2(), request.r2(), false);
        try {
            objectStorageRuntime.persistDocument(doc);
        } catch (IllegalStateException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof IllegalArgumentException) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Endpoint 格式不正确：请只填写协议与主机（如 https://<account_id>.r2.cloudflarestorage.com），桶名填在 Bucket 字段");
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存对象存储配置失败");
        }
        ObjectStorageRuntime.PingResult ping = objectStorageRuntime.pingDetail(active);
        if (!ping.success()) {
            String hint = active == StorageBackendKind.R2
                    ? "请确认页顶「当前写入后端」为 Cloudflare R2；桶已在 R2 控制台创建；令牌为 R2「对象读取与写入」且包含该桶。"
                    : "请确认页顶为 MinIO，且 Docker/本机 MinIO 已启动，或开启 MinIO「回退部署配置」。";
            String detail = ping.message() == null || ping.message().isBlank() ? "" : "（" + ping.message() + "）";
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "「" + active.name() + "」连接失败" + detail + "。" + hint);
        }
        return getSettings();
    }

    public boolean testConnection(TestPlatformObjectStorageRequest request) {
        StorageBackendKind kind = StorageBackendKind.parse(request.backend());
        if (kind != StorageBackendKind.MINIO && kind != StorageBackendKind.R2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的存储后端");
        }
        return objectStorageRuntime.ping(kind);
    }

    private void mergeBackend(ObjectStorageSettingsDocument.S3BackendSettings target,
                              UpdatePlatformObjectStorageBackendRequest request,
                              boolean allowYamlFallback) {
        if (request == null) {
            return;
        }
        if (request.endpoint() != null) {
            ObjectStorageEndpointSupport.Parsed parsed = ObjectStorageEndpointSupport.parse(
                    request.endpoint().trim(),
                    target.getBucket());
            target.setEndpoint(parsed.endpoint());
            if ((request.bucket() == null || request.bucket().isBlank()) && !parsed.bucketFromPath().isBlank()) {
                target.setBucket(parsed.bucketFromPath());
            }
        }
        if (request.accessKey() != null) {
            target.setAccessKey(request.accessKey().trim());
        }
        if (request.secretKey() != null && !request.secretKey().isBlank()) {
            target.setSecretKeyCipher(objectStorageRuntime.encryptSecret(request.secretKey().trim()));
        }
        if (request.bucket() != null) {
            target.setBucket(request.bucket().trim());
        }
        if (allowYamlFallback && request.useYamlFallback() != null) {
            target.setUseYamlFallback(request.useYamlFallback());
        }
    }

    private PlatformObjectStorageBackendVO toBackendVo(StorageBackendKind kind,
                                                     ObjectStorageSettingsDocument.S3BackendSettings settings) {
        if (settings == null) {
            settings = new ObjectStorageSettingsDocument.S3BackendSettings();
        }
        boolean hasSecret = settings.getSecretKeyCipher() != null && !settings.getSecretKeyCipher().isBlank();
        boolean yamlBacked = kind == StorageBackendKind.MINIO && settings.isUseYamlFallback();
        String endpoint = settings.getEndpoint();
        String accessKey = settings.getAccessKey();
        String bucket = settings.getBucket();
        if (yamlBacked) {
            if (endpoint == null || endpoint.isBlank()) {
                endpoint = yamlMinio.getEndpoint();
            }
            if (accessKey == null || accessKey.isBlank()) {
                accessKey = yamlMinio.getAccessKey();
            }
            if (!hasSecret) {
                hasSecret = yamlMinio.getSecretKey() != null && !yamlMinio.getSecretKey().isBlank();
            }
            if (bucket == null || bucket.isBlank()) {
                bucket = yamlMinio.getBucket();
            }
        }
        boolean configured = endpoint != null && !endpoint.isBlank()
                && accessKey != null && !accessKey.isBlank()
                && hasSecret
                && bucket != null && !bucket.isBlank();
        return new PlatformObjectStorageBackendVO(
                endpoint,
                accessKey,
                hasSecret,
                bucket,
                yamlBacked,
                configured,
                configured && objectStorageRuntime.ping(kind));
    }

    private static String buildHint(StorageBackendKind active) {
        if (active == StorageBackendKind.R2) {
            return "新上传文件写入 Cloudflare R2；历史 MinIO 对象仍按 storage_backend 读取。";
        }
        return "新上传文件写入 MinIO（可回退 application.yml）；切换后端后请确保桶与迁移策略。";
    }
}
