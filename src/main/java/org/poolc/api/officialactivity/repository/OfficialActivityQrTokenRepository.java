package org.poolc.api.officialactivity.repository;

import org.poolc.api.officialactivity.domain.OfficialActivityQrToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OfficialActivityQrTokenRepository extends JpaRepository<OfficialActivityQrToken, String> {
    Optional<OfficialActivityQrToken> findByOfficialActivityId(Long activityId);

    @Query(value = "SELECT token.activity_id AS activityId, activity.qr_enabled AS qrEnabled "
            + "FROM official_activity_qr_token token JOIN official_activity activity ON activity.id = token.activity_id "
            + "WHERE token.token = :token", nativeQuery = true)
    Optional<CheckInToken> findCheckInTokenByToken(@Param("token") String token);

    interface CheckInToken {
        Long getActivityId();
        boolean getQrEnabled();
    }
}
