package com.boxai.infrastructure.storage;

import com.boxai.domain.storage.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PublicAssetImageLoader {

    private static final Logger log = LoggerFactory.getLogger(PublicAssetImageLoader.class);

    private static final Pattern ASSET_REF = Pattern.compile(
            "/api/v1/public-assets/([0-9a-fA-F-]{36}\\.(png|jpg|jpeg|svg))");
    private static final Set<String> IMAGE_EXT = Set.of("png", "jpg", "jpeg", "svg");

    private final ObjectStorage objectStorage;
    private final String storageBucket;

    public PublicAssetImageLoader(ObjectStorage objectStorage,
                                  @Value("${box.minio.bucket:box}") String storageBucket) {
        this.objectStorage = objectStorage;
        this.storageBucket = storageBucket;
    }

    public Optional<LoadedPublicImage> loadFromReference(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = ASSET_REF.matcher(urlOrPath.trim());
        if (!matcher.find()) {
            return Optional.empty();
        }
        String fileName = matcher.group(1);
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!IMAGE_EXT.contains(ext)) {
            return Optional.empty();
        }
        try (InputStream stream = objectStorage.get(storageBucket, "public-assets/" + fileName)) {
            return Optional.of(new LoadedPublicImage(stream.readAllBytes(), contentType(ext)));
        } catch (Exception e) {
            log.warn("Failed to load public asset image {}: {}", fileName, e.getMessage());
            return Optional.empty();
        }
    }

    private static String contentType(String ext) {
        if ("png".equals(ext)) {
            return "image/png";
        }
        if ("svg".equals(ext)) {
            return "image/svg+xml";
        }
        return "image/jpeg";
    }

    public record LoadedPublicImage(byte[] bytes, String contentType) {
    }
}
