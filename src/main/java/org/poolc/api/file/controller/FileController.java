package org.poolc.api.file.controller;

import lombok.RequiredArgsConstructor;
import org.poolc.api.file.service.FileStorage;
import org.springframework.http.MediaType;
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

    @GetMapping(value = "/{fileId}/{fileName:.+}")
    public ResponseEntity sendStoredFile(@PathVariable String fileId, @PathVariable String fileName) {
        if (!isUuid(fileId) || !isSafeFileName(fileName)) {
            return ResponseEntity.badRequest().body("잘못된 파일 경로입니다");
        }

        return sendFile(fileId, fileName);
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

    private ResponseEntity sendFile(String fileId, String downloadFileName) {
        try {
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8))
                    .body(fileStorage.read(fileId));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
