package com.boxai.knowledge.support;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class DocumentTextExtractor {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "svg", "gif", "webp", "bmp");

    private final Tika tika = new Tika();

    public String extract(byte[] bytes, String fileName) throws IOException, TikaException {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String ext = extension(fileName);
        if (IMAGE_EXTENSIONS.contains(ext)) {
            // Raster images must go through vision OCR; Tika often emits PNG metadata noise (e.g. "lblb...").
            return "";
        }
        if ("txt".equals(ext) || "md".equals(ext) || "json".equals(ext)
                || "csv".equals(ext) || "log".equals(ext)) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return tika.parseToString(new ByteArrayInputStream(bytes));
    }

    private String extension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }
}
