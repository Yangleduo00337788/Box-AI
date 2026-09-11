package com.boxai.infrastructure.minio;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "box.minio")
public class MinioProperties {

    private String endpoint = "http://127.0.0.1:9000";
    private String accessKey = "box";
    private String secretKey = "boxsecret1";
    private String bucket = "box";
}
