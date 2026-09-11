package com.boxai.bootstrap.web;

import com.boxai.ai.LlmProvider;
import com.boxai.common.result.Result;
import com.boxai.infrastructure.elasticsearch.ElasticsearchService;
import com.boxai.infrastructure.minio.MinioService;
import com.boxai.infrastructure.redis.RedisService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final RedisService redisService;
    private final ElasticsearchService elasticsearchService;
    private final MinioService minioService;
    private final LlmProvider llmProvider;

    public HealthController(JdbcTemplate jdbcTemplate,
                            RedisService redisService,
                            ElasticsearchService elasticsearchService,
                            MinioService minioService,
                            LlmProvider llmProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisService = redisService;
        this.elasticsearchService = elasticsearchService;
        this.minioService = minioService;
        this.llmProvider = llmProvider;
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("mysql", pingMysql());
        checks.put("redis", ping(() -> redisService.ping()));
        checks.put("elasticsearch", ping(elasticsearchService::ping));
        checks.put("minio", ping(minioService::ping));
        checks.put("llmProvider", Map.of("name", llmProvider.providerName(), "configured", llmProvider.available()));
        boolean up = Boolean.TRUE.equals(checks.get("mysql")) && Boolean.TRUE.equals(checks.get("redis"));
        checks.put("status", up ? "UP" : "DEGRADED");
        return Result.success(checks);
    }

    private boolean pingMysql() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Integer.valueOf(1).equals(one);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean ping(Ping ping) {
        try {
            return ping.check();
        } catch (Exception e) {
            return false;
        }
    }

    @FunctionalInterface
    private interface Ping {
        boolean check();
    }
}
