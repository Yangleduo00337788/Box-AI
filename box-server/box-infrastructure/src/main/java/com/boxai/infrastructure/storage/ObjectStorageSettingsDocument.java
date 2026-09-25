package com.boxai.infrastructure.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectStorageSettingsDocument {

    private String active = "MINIO";
    private S3BackendSettings minio = new S3BackendSettings();
    private S3BackendSettings r2 = new S3BackendSettings();

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class S3BackendSettings {
        private String endpoint;
        private String accessKey;
        /** AES-GCM 密文；管理端保存时写入。 */
        private String secretKeyCipher;
        private String bucket;
        /** 为 true 时 endpoint/密钥/bucket 未配置则回退 application.yml 的 box.minio */
        private boolean useYamlFallback = true;
    }
}
