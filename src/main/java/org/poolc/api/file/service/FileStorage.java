package org.poolc.api.file.service;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorage {
    void store(String fileId, InputStream inputStream, long contentLength, String contentType) throws IOException;

    byte[] read(String fileId) throws IOException;
}
