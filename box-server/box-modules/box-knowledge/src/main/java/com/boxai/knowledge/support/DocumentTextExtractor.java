package com.boxai.knowledge.support;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class DocumentTextExtractor {

    private final Tika tika = new Tika();

    public String extract(byte[] bytes, String fileName) throws IOException, TikaException {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".json")
                || lower.endsWith(".csv") || lower.endsWith(".log")) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return tika.parseToString(new ByteArrayInputStream(bytes));
    }
}
