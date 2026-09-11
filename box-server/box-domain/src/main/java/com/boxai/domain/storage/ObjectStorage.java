package com.boxai.domain.storage;

import java.io.InputStream;

public interface ObjectStorage {

    void put(String bucket, String key, InputStream data, long size, String contentType);

    InputStream get(String bucket, String key);

    void delete(String bucket, String key);
}
