package org.poolc.api.file.service;

import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
public class FileReferenceRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Set<String> findAllReferencedFileUrls() {
        List<?> values = entityManager.createNativeQuery(
                "select thumbnail_url from project where thumbnail_url is not null " +
                        "union all select image_url from book where image_url is not null " +
                        "union all select image_url from badge where image_url is not null " +
                        "union all select location_url from poolc where location_url is not null " +
                        "union all select main_image_url from poolc where main_image_url is not null " +
                        "union all select file_uri from post_file_list where file_uri is not null " +
                        "union all select file_uri from activity_file_list where file_uri is not null " +
                        "union all select file_uri from session_file_list where file_uri is not null"
        ).getResultList();

        Set<String> urls = new LinkedHashSet<>();
        for (Object value : values) {
            if (value != null) {
                urls.add(value.toString());
            }
        }
        return urls;
    }
}
