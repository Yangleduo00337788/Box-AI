package com.boxai.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * CI/Linux: Testcontainers (default). Windows 本地 Docker Desktop 与 Testcontainers 的 npipe 常不兼容，
 * 默认走 {@code deploy/docker-compose} 的 MySQL(3307)/Redis(6379)；可用 {@code -Dbox.it.external=false} 强制容器。
 */
final class IntegrationTestInfrastructure {

    private static final String JDBC_PARAMS =
            "useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";

    private static final boolean USE_EXTERNAL = resolveUseExternal();

    private static final MySQLContainer<?> MYSQL;
    private static final GenericContainer<?> REDIS;

    static {
        if (USE_EXTERNAL) {
            MYSQL = null;
            REDIS = null;
        } else {
            MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
                    .withDatabaseName("box")
                    .withUsername("box")
                    .withPassword("box");
            REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                    .withExposedPorts(6379);
            MYSQL.start();
            REDIS.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                REDIS.stop();
                MYSQL.stop();
            }));
        }
    }

    private IntegrationTestInfrastructure() {
    }

    static void registerDataSources(DynamicPropertyRegistry registry) {
        if (USE_EXTERNAL) {
            String jdbcUrl = envOrDefault(
                    "BOX_IT_JDBC_URL",
                    "jdbc:mysql://127.0.0.1:3307/box?" + JDBC_PARAMS);
            String jdbcUser = envOrDefault("BOX_IT_JDBC_USER", "root");
            String jdbcPassword = envOrDefault("BOX_IT_JDBC_PASSWORD", "box");
            registry.add("spring.datasource.url", () -> jdbcUrl);
            registry.add("spring.datasource.username", () -> jdbcUser);
            registry.add("spring.datasource.password", () -> jdbcPassword);
            registry.add("spring.data.redis.host", () -> envOrDefault("BOX_IT_REDIS_HOST", "127.0.0.1"));
            registry.add("spring.data.redis.port", () -> Integer.parseInt(envOrDefault("BOX_IT_REDIS_PORT", "6379")));
            registry.add("spring.data.redis.password", () -> envOrDefault("BOX_IT_REDIS_PASSWORD", envOrDefault("SPRING_DATA_REDIS_PASSWORD", "")));
            registerLocalStackOverrides(registry);
        } else {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
            registry.add("spring.data.redis.host", REDIS::getHost);
            registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        }
    }

    private static boolean resolveUseExternal() {
        String explicit = System.getProperty("box.it.external");
        if (explicit != null && !explicit.isBlank()) {
            return Boolean.parseBoolean(explicit.trim());
        }
        String fromEnv = System.getenv("BOX_IT_EXTERNAL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Boolean.parseBoolean(fromEnv.trim());
        }
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 本地共用 MySQL 时，库内平台配置往往用 dev 密钥加密；与 {@code application-integration.yml} 的测试密钥不一致会启动失败。
     * CI 使用空库 + integration 密钥即可；本地 external 默认对齐 dev 密钥，可用环境变量覆盖。
     */
    private static void registerLocalStackOverrides(DynamicPropertyRegistry registry) {
        registry.add("box.security.crypto.aes-key",
                () -> envOrDefault("BOX_IT_AES_KEY", "box-dev-aes-256-key-change-me!!"));
        registry.add("box.security.jwt.secret",
                () -> envOrDefault("BOX_IT_JWT_SECRET", "box-dev-jwt-secret-change-me-please-32b"));
        registerIfPresent(registry, "box.minio.endpoint", "BOX_IT_MINIO_ENDPOINT");
        registerIfPresent(registry, "box.minio.access-key", "BOX_IT_MINIO_ACCESS_KEY");
        registerIfPresent(registry, "box.minio.secret-key", "BOX_IT_MINIO_SECRET_KEY");
        registerIfPresent(registry, "box.minio.bucket", "BOX_IT_MINIO_BUCKET");
    }

    private static void registerIfPresent(DynamicPropertyRegistry registry, String property, String envKey) {
        String value = System.getenv(envKey);
        if (value != null && !value.isBlank()) {
            registry.add(property, () -> value);
        }
    }
}
