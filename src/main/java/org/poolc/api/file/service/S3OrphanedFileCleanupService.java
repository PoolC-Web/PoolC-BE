package org.poolc.api.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "file.storage", havingValue = "S3")
public class S3OrphanedFileCleanupService {
    private static final String OBJECTS_PREFIX = "objects/";
    private static final String PREVIEWS_PREFIX = "previews/";
    private static final Pattern FILE_URL_PATTERN = Pattern.compile("(?:^|/)files/([0-9a-fA-F-]{36})(?:/|$)");
    private static final Pattern OBJECT_KEY_PATTERN = Pattern.compile("^objects/([0-9a-fA-F-]{36})$");
    private static final Pattern PREVIEW_KEY_PATTERN = Pattern.compile("^previews/([0-9a-fA-F-]{36})/(?:card|detail)\\.webp$");

    private final S3Client s3Client;
    private final FileReferenceRepository fileReferenceRepository;

    @Value("${file.s3.bucket}")
    private String bucket;

    @Value("${file.orphan-cleanup.enabled:false}")
    private boolean enabled;

    @Value("${file.orphan-cleanup.dry-run:true}")
    private boolean dryRun;

    @Value("${file.orphan-cleanup.grace-period-hours:168}")
    private long gracePeriodHours;

    @Scheduled(cron = "${file.orphan-cleanup.cron:0 30 4 * * *}", zone = "Asia/Seoul")
    public void cleanOrphanedFiles() {
        if (!enabled) {
            return;
        }

        Set<String> referencedFileIds = referencedFileIds();
        Instant cutoff = Instant.now().minus(gracePeriodHours, ChronoUnit.HOURS);
        Set<String> orphanedKeys = new HashSet<>();
        collectOrphanedKeys(OBJECTS_PREFIX, OBJECT_KEY_PATTERN, referencedFileIds, cutoff, orphanedKeys);
        collectOrphanedKeys(PREVIEWS_PREFIX, PREVIEW_KEY_PATTERN, referencedFileIds, cutoff, orphanedKeys);

        if (orphanedKeys.isEmpty()) {
            log.info("S3 orphan cleanup completed: no candidates");
            return;
        }

        if (dryRun) {
            log.info("S3 orphan cleanup dry-run: {} keys eligible after {} hours", orphanedKeys.size(), gracePeriodHours);
            return;
        }

        int deleted = 0;
        for (String key : orphanedKeys) {
            try {
                s3Client.deleteObject(builder -> builder.bucket(bucket).key(key));
                deleted++;
            } catch (S3Exception exception) {
                log.warn("Failed to delete orphaned S3 object {}", key, exception);
            }
        }
        log.info("S3 orphan cleanup completed: deleted {} of {} keys", deleted, orphanedKeys.size());
    }

    private Set<String> referencedFileIds() {
        Set<String> fileIds = new HashSet<>();
        for (String url : fileReferenceRepository.findAllReferencedFileUrls()) {
            Matcher matcher = FILE_URL_PATTERN.matcher(url);
            if (matcher.find()) {
                fileIds.add(UUID.fromString(matcher.group(1)).toString());
            }
        }
        return fileIds;
    }

    private void collectOrphanedKeys(String prefix, Pattern keyPattern, Set<String> referencedFileIds,
                                     Instant cutoff, Set<String> orphanedKeys) {
        String continuationToken = null;
        do {
            ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .continuationToken(continuationToken)
                    .build());
            for (S3Object object : response.contents()) {
                Matcher matcher = keyPattern.matcher(object.key());
                if (!matcher.matches() || object.lastModified().isAfter(cutoff)) {
                    continue;
                }
                String fileId = UUID.fromString(matcher.group(1)).toString();
                if (!referencedFileIds.contains(fileId)) {
                    orphanedKeys.add(object.key());
                }
            }
            continuationToken = response.nextContinuationToken();
        } while (continuationToken != null);
    }
}
