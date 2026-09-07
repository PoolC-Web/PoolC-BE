package org.poolc.api.file.service;

import java.io.IOException;
import java.io.InputStream;

public class StoredFile implements AutoCloseable {
    private final InputStream inputStream;
    private final long contentLength;
    private final String contentType;

    public StoredFile(InputStream inputStream, long contentLength, String contentType) {
        this.inputStream = inputStream;
        this.contentLength = contentLength;
        this.contentType = contentType;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
