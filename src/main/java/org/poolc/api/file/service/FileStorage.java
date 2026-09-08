package org.poolc.api.file.service;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorage {
    void store(String fileId, InputStream inputStream, long contentLength, String contentType) throws IOException;

    StoredFile load(String fileId) throws IOException;

    void storePreview(String fileId, ImageVariant variant, InputStream inputStream, long contentLength, String contentType) throws IOException;

    StoredFile loadPreview(String fileId, ImageVariant variant) throws IOException;
}
