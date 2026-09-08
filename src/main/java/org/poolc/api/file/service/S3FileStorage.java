package org.poolc.api.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "file.storage", havingValue = "S3")
public class S3FileStorage implements FileStorage {
    private static final String OBJECTS_PREFIX = "objects/";

    private final S3Client s3Client;
    private final String bucket;

    public S3FileStorage(S3Client s3Client, @Value("${file.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void store(String fileId, InputStream inputStream, long contentLength, String contentType) throws IOException {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey(fileId))
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (S3Exception e) {
            throw new IOException("S3 파일 업로드에 실패했습니다", e);
        }
    }

    @Override
    public byte[] read(String fileId) throws IOException {
        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey(fileId))
                .build())) {
            return response.readAllBytes();
        } catch (S3Exception e) {
            throw new IOException("존재하지 않는 파일입니다", e);
        }
    }

    private String objectKey(String fileId) {
        return OBJECTS_PREFIX + fileId;
    }
}
