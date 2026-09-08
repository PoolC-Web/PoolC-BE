package org.poolc.api.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@ConditionalOnProperty(name = "file.storage", havingValue = "LOCAL", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {
    private static final String OBJECTS_DIRECTORY = "objects";
    private static final String PREVIEWS_DIRECTORY = "previews";
    private static final String TEMP_DIRECTORY = "temp";

    private final Path storageRoot;

    public LocalFileStorage(@Value("${file.file-dir}") String fileDir) {
        this.storageRoot = Paths.get(fileDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    private void initializeDirectories() throws IOException {
        Files.createDirectories(objectsDirectory());
        Files.createDirectories(storageRoot.resolve(PREVIEWS_DIRECTORY));
        Files.createDirectories(storageRoot.resolve(TEMP_DIRECTORY));
    }

    @Override
    public void store(String fileId, InputStream inputStream, long contentLength, String contentType) throws IOException {
        Files.copy(inputStream, objectsDirectory().resolve(fileId));
    }

    @Override
    public StoredFile load(String fileId) throws IOException {
        Path path = objectsDirectory().resolve(fileId);
        if (!Files.isRegularFile(path)) {
            throw new IOException("존재하지 않는 파일입니다");
        }
        return new StoredFile(Files.newInputStream(path), Files.size(path), Files.probeContentType(path));
    }

    @Override
    public void storePreview(String fileId, ImageVariant variant, InputStream inputStream, long contentLength, String contentType) throws IOException {
        Path previewPath = previewPath(fileId, variant);
        Files.createDirectories(previewPath.getParent());
        Files.copy(inputStream, previewPath);
    }

    @Override
    public StoredFile loadPreview(String fileId, ImageVariant variant) throws IOException {
        Path path = previewPath(fileId, variant);
        if (!Files.isRegularFile(path)) {
            throw new IOException("존재하지 않는 미리보기 파일입니다");
        }
        return new StoredFile(Files.newInputStream(path), Files.size(path), Files.probeContentType(path));
    }

    private Path objectsDirectory() {
        return storageRoot.resolve(OBJECTS_DIRECTORY);
    }

    private Path previewPath(String fileId, ImageVariant variant) {
        return storageRoot.resolve(PREVIEWS_DIRECTORY).resolve(fileId).resolve(variant.name().toLowerCase() + ".webp");
    }
}
