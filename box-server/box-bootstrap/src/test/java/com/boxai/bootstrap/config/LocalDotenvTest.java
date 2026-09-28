package com.boxai.bootstrap.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LocalDotenvTest {

    @Test
    void parseIgnoresCommentsAndUnquotes() {
        Map<String, String> values = LocalDotenv.parse("""
                # comment
                BOX_SECURITY_JWT_SECRET=plain
                export BOX_MINIO_SECRET_KEY="quoted"
                BOX_MAIL_HOST=
                invalid
                """);
        assertEquals("plain", values.get("BOX_SECURITY_JWT_SECRET"));
        assertEquals("quoted", values.get("BOX_MINIO_SECRET_KEY"));
        assertEquals("", values.get("BOX_MAIL_HOST"));
        assertFalse(values.containsKey("invalid"));
    }

    @Test
    void skipsBlankMailHostSoMailAutoConfigStaysOff() {
        Map<String, Object> properties = LocalDotenv.toPropertyMap(Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:mysql://127.0.0.1:3306/box",
                "BOX_MAIL_HOST", ""));
        assertFalse(properties.containsKey("spring.mail.host"));
        assertFalse(properties.containsKey("BOX_MAIL_HOST"));
    }

    @Test
    void aliasesDatasourceUrlForSpringBinder() {
        Map<String, Object> properties = LocalDotenv.toPropertyMap(Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:mysql://127.0.0.1:3306/box"));
        assertEquals("jdbc:mysql://127.0.0.1:3306/box", properties.get("SPRING_DATASOURCE_URL"));
        assertEquals("jdbc:mysql://127.0.0.1:3306/box", properties.get("spring.datasource.url"));
    }
}
