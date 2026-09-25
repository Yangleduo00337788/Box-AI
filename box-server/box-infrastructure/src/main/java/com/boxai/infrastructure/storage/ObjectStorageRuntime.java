package com.boxai.infrastructure.storage;

import com.boxai.domain.config.SystemConfig;
import com.boxai.domain.config.SystemConfigRepository;
import com.boxai.domain.crypto.SecretCipher;
import com.boxai.domain.storage.StorageBackendKind;
import com.boxai.infrastructure.minio.MinioProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ObjectStorageRuntime {

    public static final String CONFIG_KEY = "platform.object_storage.settings";

    private final SystemConfigRepository systemConfigRepository;
    private final SecretCipher secretCipher;
    private final MinioProperties yamlMinio;
    private final ObjectMapper objectMapper;

    private volatile StorageBackendKind active = StorageBackendKind.MINIO;
    private volatile String activeBucket = "box";
    private volatile Map<StorageBackendKind, BackendClient> backends = Map.of();

    public ObjectStorageRuntime(SystemConfigRepository systemConfigRepository,
                                SecretCipher secretCipher,
                                MinioProperties yamlMinio,
                                ObjectMapper objectMapper) {
        this.systemConfigRepository = systemConfigRepository;
        this.secretCipher = secretCipher;
        this.yamlMinio = yamlMinio;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    public synchronized void reload() {
        ObjectStorageSettingsDocument doc = loadDocument();
        EnumMap<StorageBackendKind, BackendClient> next = new EnumMap<>(StorageBackendKind.class);
        next.put(StorageBackendKind.MINIO, buildBackend(StorageBackendKind.MINIO, doc.getMinio()));
        next.put(StorageBackendKind.R2, buildBackend(StorageBackendKind.R2, doc.getR2()));
        this.backends = next;
        this.active = StorageBackendKind.parse(doc.getActive());
        BackendClient activeClient = next.get(this.active);
        if (activeClient == null || !activeClient.configured()) {
            this.active = StorageBackendKind.MINIO;
            activeClient = next.get(StorageBackendKind.MINIO);
        }
        this.activeBucket = activeClient != null ? activeClient.bucket() : yamlMinio.getBucket();
    }

    public StorageBackendKind activeKind() {
        return active;
    }

    public String activeBucket() {
        return activeBucket;
    }

    public ObjectStorageSettingsDocument snapshotDocument() {
        return loadDocument();
    }

    public void persistDocument(ObjectStorageSettingsDocument document) {
        try {
            String json = objectMapper.writeValueAsString(document);
            SystemConfig config = new SystemConfig();
            config.setConfigKey(CONFIG_KEY);
            config.setConfigValue(json);
            config.setDescription("平台对象存储：MinIO / Cloudflare R2 与当前激活后端");
            systemConfigRepository.upsert(config);
            reload();
        } catch (Exception e) {
            throw new IllegalStateException("保存对象存储配置失败", e);
        }
    }

    public ObjectStorageBackend delegate(StorageBackendKind kind) {
        BackendClient client = backends.get(kind);
        if (client == null || !client.configured()) {
            throw new IllegalStateException("对象存储后端未配置: " + kind);
        }
        return client.storage();
    }

    public StorageBackendKind resolveReadBackend(String backend) {
        if (backend == null || backend.isBlank()) {
            return StorageBackendKind.MINIO;
        }
        return StorageBackendKind.parse(backend);
    }

    public boolean ping(StorageBackendKind kind) {
        return pingDetail(kind).success();
    }

    public PingResult pingDetail(StorageBackendKind kind) {
        try {
            boolean ok = delegate(kind).ping();
            if (ok) {
                return PingResult.ok();
            }
            return PingResult.fail("无法访问存储桶，请检查 Endpoint、密钥与 Bucket 名称");
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return PingResult.fail(message);
        }
    }

    public record PingResult(boolean success, String message) {
        static PingResult ok() {
            return new PingResult(true, "");
        }

        static PingResult fail(String message) {
            return new PingResult(false, message);
        }
    }

    public String decryptSecret(String cipher) {
        if (cipher == null || cipher.isBlank()) {
            return "";
        }
        return secretCipher.decrypt(cipher);
    }

    public String encryptSecret(String plain) {
        if (plain == null || plain.isBlank()) {
            return "";
        }
        return secretCipher.encrypt(plain);
    }

    private ObjectStorageSettingsDocument loadDocument() {
        Optional<String> raw = systemConfigRepository.findByKey(CONFIG_KEY).map(SystemConfig::getConfigValue);
        if (raw.isEmpty() || raw.get().isBlank()) {
            ObjectStorageSettingsDocument defaults = new ObjectStorageSettingsDocument();
            defaults.setActive(StorageBackendKind.MINIO.name());
            ObjectStorageSettingsDocument.S3BackendSettings minio = defaults.getMinio();
            minio.setUseYamlFallback(true);
            minio.setBucket(yamlMinio.getBucket());
            return defaults;
        }
        try {
            return objectMapper.readValue(raw.get(), ObjectStorageSettingsDocument.class);
        } catch (Exception e) {
            ObjectStorageSettingsDocument defaults = new ObjectStorageSettingsDocument();
            defaults.getMinio().setUseYamlFallback(true);
            return defaults;
        }
    }

    private BackendClient buildBackend(StorageBackendKind kind, ObjectStorageSettingsDocument.S3BackendSettings settings) {
        if (settings == null) {
            settings = new ObjectStorageSettingsDocument.S3BackendSettings();
        }
        ResolvedS3 resolved = resolveS3(kind, settings);
        if (!resolved.configured()) {
            return new BackendClient(kind, resolved.bucket(), null);
        }
        ObjectStorageBackend storage;
        if (kind == StorageBackendKind.R2) {
            storage = new AwsS3R2ObjectStorage(
                    resolved.endpoint(), resolved.accessKey(), resolved.secretKey(), resolved.bucket());
        } else {
            MinioClient client = MinioClient.builder()
                    .endpoint(resolved.endpoint())
                    .credentials(resolved.accessKey(), resolved.secretKey())
                    .build();
            storage = new MinioObjectStorageBackend(client, resolved.bucket());
        }
        return new BackendClient(kind, resolved.bucket(), storage);
    }

    private ResolvedS3 resolveS3(StorageBackendKind kind, ObjectStorageSettingsDocument.S3BackendSettings settings) {
        String endpoint = trim(settings.getEndpoint());
        String accessKey = trim(settings.getAccessKey());
        String secretKey = decryptSecret(settings.getSecretKeyCipher());
        String bucket = trim(settings.getBucket());

        if (kind == StorageBackendKind.MINIO && settings.isUseYamlFallback()) {
            if (endpoint.isEmpty()) {
                endpoint = yamlMinio.getEndpoint();
            }
            if (accessKey.isEmpty()) {
                accessKey = yamlMinio.getAccessKey();
            }
            if (secretKey.isEmpty()) {
                secretKey = yamlMinio.getSecretKey();
            }
            if (bucket.isEmpty()) {
                bucket = yamlMinio.getBucket();
            }
        }

        ObjectStorageEndpointSupport.Parsed endpointParsed = ObjectStorageEndpointSupport.parse(endpoint, bucket);
        endpoint = endpointParsed.endpoint();
        bucket = endpointParsed.bucketFromPath();

        boolean configured = !endpoint.isEmpty() && !accessKey.isEmpty() && !secretKey.isEmpty() && !bucket.isEmpty();
        return new ResolvedS3(endpoint, accessKey, secretKey, bucket, configured);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private record ResolvedS3(String endpoint, String accessKey, String secretKey, String bucket, boolean configured) {
    }

    private record BackendClient(StorageBackendKind kind, String bucket, ObjectStorageBackend storage) {

        boolean configured() {
            return storage != null;
        }
    }
}
