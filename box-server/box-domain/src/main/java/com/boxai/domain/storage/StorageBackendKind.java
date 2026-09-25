package com.boxai.domain.storage;

import java.util.Locale;

public enum StorageBackendKind {

    MINIO,
    R2;

    public static StorageBackendKind parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return MINIO;
        }
        return StorageBackendKind.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
