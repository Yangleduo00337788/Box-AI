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
}
