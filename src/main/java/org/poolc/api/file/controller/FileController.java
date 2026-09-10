package org.poolc.api.file.controller;

import lombok.RequiredArgsConstructor;
import org.poolc.api.file.service.FileStorage;
import org.poolc.api.file.service.ImageVariant;
import org.poolc.api.file.service.StoredFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {
    private final FileStorage fileStorage;

    @Value("${file.image.max-size-bytes:10000000}")
    private long maxImageFileSize;

    @GetMapping(value = "/{fileId}/{fileName:.+}")
    public ResponseEntity sendStoredFile(@PathVariable String fileId, @PathVariable String fileName, @RequestParam(required = false) ImageVariant variant) {
        if (!isUuid(fileId) || !isSafeFileName(fileName)) {
            return ResponseEntity.badRequest().body("잘못된 파일 경로입니다");
        }

        return sendFile(fileId, fileName, variant);
    }

    /**
     * 기존 /files/{fileName} 링크를 유지한다. 새 파일은 objects/ 아래에 저장된다.
     */
    @GetMapping(value = "/{fileName:.+}")
    public ResponseEntity sendLegacyFile(@PathVariable String fileName) {
        if (!isSafeFileName(fileName)) {
            return ResponseEntity.badRequest().body("잘못된 파일 경로입니다");
        }

        return ResponseEntity.badRequest().body("기존 파일 URL은 마이그레이션 후 사용할 수 없습니다");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> uploadFile(@ModelAttribute MultipartFile file) {
        try {
            if (isImage(file) && file.getSize() > maxImageFileSize) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("이미지 파일은 10MB 이하만 업로드할 수 있습니다");
            }
            String originalFileName = sanitizeFileName(file.getOriginalFilename());
            if (originalFileName.isEmpty()) {
                return ResponseEntity.badRequest().body("파일명이 없습니다");
            }

            String fileId = UUID.randomUUID().toString();
            fileStorage.store(fileId, file.getInputStream(), file.getSize(), file.getContentType());
            String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8);
            return ResponseEntity.ok().body("/files/" + fileId + "/" + encodedFileName);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> uploadImage(@RequestPart("original") MultipartFile original,
                                              @RequestPart("card") MultipartFile card,
                                              @RequestPart("detail") MultipartFile detail) {
        try {
            if (!isImage(original) || !isImage(card) || !isImage(detail)) {
                return ResponseEntity.badRequest().body("원본과 미리보기는 이미지 파일이어야 합니다");
            }
            if (original.getSize() > maxImageFileSize) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("이미지 파일은 10MB 이하만 업로드할 수 있습니다");
            }

            String originalFileName = sanitizeFileName(original.getOriginalFilename());
            if (originalFileName.isEmpty()) {
                return ResponseEntity.badRequest().body("파일명이 없습니다");
            }

            String fileId = UUID.randomUUID().toString();
            fileStorage.store(fileId, original.getInputStream(), original.getSize(), original.getContentType());
            fileStorage.storePreview(fileId, ImageVariant.CARD, card.getInputStream(), card.getSize(), card.getContentType());
            fileStorage.storePreview(fileId, ImageVariant.DETAIL, detail.getInputStream(), detail.getSize(), detail.getContentType());
            String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8);
            return ResponseEntity.ok().body("/files/" + fileId + "/" + encodedFileName);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private ResponseEntity<?> sendFile(String fileId, String downloadFileName, ImageVariant variant) {
        try (StoredFile storedFile = loadFile(fileId, variant)) {
            MediaType contentType = resolveContentType(storedFile.getContentType(), downloadFileName);
            byte[] content = storedFile.getInputStream().readAllBytes();

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .contentLength(content.length)
                    .header("Cache-Control", "public, max-age=31536000, immutable")
                    .header("Content-Disposition", ContentDisposition.inline().filename(downloadFileName, StandardCharsets.UTF_8).build().toString())
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private StoredFile loadFile(String fileId, ImageVariant variant) throws IOException {
        if (variant == null) {
            return fileStorage.load(fileId);
        }
        try {
            return fileStorage.loadPreview(fileId, variant);
        } catch (IOException ignored) {
            return fileStorage.load(fileId);
        }
    }

    private MediaType resolveContentType(String storedContentType, String fileName) {
        if (storedContentType != null && !storedContentType.isBlank()) {
            try {
                return MediaType.parseMediaType(storedContentType);
            } catch (IllegalArgumentException ignored) {
                // 이전에 잘못 저장된 메타데이터는 파일 확장자로 보완한다.
            }
        }
        return MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private boolean isImage(MultipartFile file) {
        return file.getContentType() != null && file.getContentType().startsWith("image/");
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null) {
            return "";
        }
        String normalized = originalFileName.replace('\\', '/');
        Path path = Paths.get(normalized).getFileName();
        if (path == null) {
            return "";
        }
        String fileName = path.toString().replace(' ', '-');
        return isSafeFileName(fileName) ? fileName : "";
    }

    private boolean isSafeFileName(String fileName) {
        return fileName != null && !fileName.isBlank() && !fileName.equals(".") && !fileName.equals("..")
                && !fileName.contains("/") && !fileName.contains("\\");
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
