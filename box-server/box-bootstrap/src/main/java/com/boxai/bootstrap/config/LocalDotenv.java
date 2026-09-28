package com.boxai.bootstrap.config;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 从仓库/.env 读取本地环境变量。已存在的系统环境变量优先，不会被覆盖。
 */
public final class LocalDotenv {

    static final String PROPERTY_SOURCE_NAME = "boxDotenv";
    static final String DISABLE_PROPERTY = "box.dotenv.disabled";
    static final String DISABLE_ENV = "BOX_DOTENV_DISABLED";
    static final String FILE_ENV = "BOX_ENV_FILE";

    private static final Map<String, String> SPRING_ALIASES = Map.ofEntries(
            Map.entry("SERVER_PORT", "server.port"),
            Map.entry("SPRING_DATASOURCE_URL", "spring.datasource.url"),
            Map.entry("SPRING_DATASOURCE_USERNAME", "spring.datasource.username"),
            Map.entry("SPRING_DATASOURCE_PASSWORD", "spring.datasource.password"),
            Map.entry("SPRING_DATA_REDIS_HOST", "spring.data.redis.host"),
            Map.entry("SPRING_DATA_REDIS_PORT", "spring.data.redis.port"),
            Map.entry("BOX_SECURITY_JWT_SECRET", "box.security.jwt.secret"),
            Map.entry("BOX_SECURITY_CRYPTO_AES_KEY", "box.security.crypto.aes-key"),
            Map.entry("BOX_MINIO_ENDPOINT", "box.minio.endpoint"),
            Map.entry("BOX_MINIO_ACCESS_KEY", "box.minio.access-key"),
            Map.entry("BOX_MINIO_SECRET_KEY", "box.minio.secret-key"),
            Map.entry("BOX_MINIO_BUCKET", "box.minio.bucket"),
            Map.entry("BOX_ELASTICSEARCH_HOST", "box.elasticsearch.host"),
            Map.entry("BOX_ELASTICSEARCH_PORT", "box.elasticsearch.port"),
            Map.entry("BOX_ELASTICSEARCH_SCHEME", "box.elasticsearch.scheme"),
            Map.entry("BOX_ELASTICSEARCH_USERNAME", "box.elasticsearch.username"),
            Map.entry("BOX_ELASTICSEARCH_PASSWORD", "box.elasticsearch.password"),
            Map.entry("BOX_ELASTICSEARCH_TRUST_INSECURE", "box.elasticsearch.trust-insecure-certificate"),
            Map.entry("BOX_ELASTICSEARCH_ENABLED", "box.elasticsearch.enabled"),
            Map.entry("BOX_MAIL_HOST", "spring.mail.host"),
            Map.entry("BOX_MAIL_PORT", "spring.mail.port"),
            Map.entry("BOX_MAIL_USERNAME", "spring.mail.username"),
            Map.entry("BOX_MAIL_PASSWORD", "spring.mail.password"),
            Map.entry("BOX_MAIL_FROM", "box.mail.from"),
            Map.entry("BOX_AUTH_VERIFICATION_EXPOSE_CODE", "box.auth.verification.expose-code"),
            Map.entry("BOX_EMBED_SKIP_DOMAIN_VERIFY", "box.embed.skip-domain-verify"),
            Map.entry("BOX_OPENAI_API_KEY", "box.ai.openai.api-key")
    );

    private LocalDotenv() {
    }

    public static void apply(ConfigurableEnvironment environment) {
        if (disabled()) {
            return;
        }
        Optional<Path> file = resolveFile();
        if (file.isEmpty()) {
            System.out.println("[box] 未找到 .env（可从仓库根目录复制 .env.example）");
            return;
        }
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        try {
            Map<String, Object> properties = toPropertyMap(loadNewValues(file.get()));
            if (properties.isEmpty()) {
                return;
            }
            if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
                environment.getPropertySources().addAfter(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
            } else {
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
            }
            System.out.println("[box] 已加载环境变量文件: " + file.get());
        } catch (IOException ex) {
            throw new IllegalStateException("无法读取环境变量文件: " + file.get(), ex);
        }
    }

    static boolean disabled() {
        if (truthy(System.getenv(DISABLE_ENV))) {
            return true;
        }
        if (Boolean.parseBoolean(System.getProperty(DISABLE_PROPERTY, "false"))) {
            return true;
        }
        return System.getProperty("surefire.test.class.path") != null
                || System.getProperty("failsafe.test.class.path") != null;
    }

    static Optional<Path> resolveFile() {
        String explicit = firstNonBlank(System.getenv(FILE_ENV), System.getProperty(FILE_ENV));
        if (explicit != null) {
            Path path = Path.of(explicit.trim());
            return Files.isRegularFile(path) ? Optional.of(path.toAbsolutePath().normalize()) : Optional.empty();
        }
        Path cursor = Path.of("").toAbsolutePath().normalize();
        for (int i = 0; i < 8 && cursor != null; i++) {
            Path candidate = cursor.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
            Path parent = cursor.getParent();
            if (parent == null || parent.equals(cursor)) {
                break;
            }
            cursor = parent;
        }
        return Optional.empty();
    }

    static Map<String, String> loadNewValues(Path file) throws IOException {
        Map<String, String> parsed = parse(Files.readString(file, StandardCharsets.UTF_8));
        Map<String, String> fresh = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : parsed.entrySet()) {
            String existing = System.getenv(entry.getKey());
            if (existing != null && !existing.isBlank()) {
                continue;
            }
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            fresh.put(entry.getKey(), entry.getValue());
        }
        return fresh;
    }

    static Map<String, Object> toPropertyMap(Map<String, String> values) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            properties.put(entry.getKey(), entry.getValue());
            String alias = SPRING_ALIASES.get(entry.getKey());
            if (alias != null) {
                properties.put(alias, entry.getValue());
            }
            if ("BOX_MAIL_HOST".equals(entry.getKey())) {
                properties.put("spring.mail.properties.mail.smtp.auth", "true");
                properties.put("spring.mail.properties.mail.smtp.starttls.enable", "true");
            }
        }
        return properties;
    }

    static Map<String, String> parse(String content) {
        Map<String, String> values = new LinkedHashMap<>();
        if (content == null || content.isBlank()) {
            return values;
        }
        String text = content.startsWith("\uFEFF") ? content.substring(1) : content;
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.regionMatches(true, 0, "export ", 0, 7)) {
                line = line.substring(7).trim();
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            if (key.isEmpty()) {
                continue;
            }
            values.put(key, unquote(line.substring(eq + 1).trim()));
        }
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static boolean truthy(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
