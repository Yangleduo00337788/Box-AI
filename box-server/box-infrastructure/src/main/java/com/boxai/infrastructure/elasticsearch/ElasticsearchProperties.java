package com.boxai.infrastructure.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "box.elasticsearch")
public class ElasticsearchProperties {

    private String host = "127.0.0.1";
    private int port = 9200;
    /** http or https (Elasticsearch 8 Docker 默认 https) */
    private String scheme = "http";
    private String username;
    private String password;
    /** 本地自签证书（如 Docker ES）时设为 true */
    private boolean trustInsecureCertificate = false;
    /**
     * When false, skip all Elasticsearch HTTP calls (MySQL keyword fallback still works for RAG).
     */
    private boolean enabled = true;
}
