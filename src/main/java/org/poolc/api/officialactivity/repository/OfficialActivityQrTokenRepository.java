package org.poolc.api.officialactivity.repository;

import org.poolc.api.officialactivity.domain.OfficialActivityQrToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfficialActivityQrTokenRepository extends JpaRepository<OfficialActivityQrToken, String> {
    Optional<OfficialActivityQrToken> findByOfficialActivityId(Long activityId);
}
