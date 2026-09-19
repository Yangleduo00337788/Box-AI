package com.boxai.infrastructure.elasticsearch;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchService {

    private final ElasticsearchHttpClient elasticsearchHttpClient;

    public ElasticsearchService(ElasticsearchHttpClient elasticsearchHttpClient) {
        this.elasticsearchHttpClient = elasticsearchHttpClient;
    }

    public boolean ping() {
        return elasticsearchHttpClient.ping();
    }
}
