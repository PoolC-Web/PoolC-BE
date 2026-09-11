package org.poolc.api.officialactivity.repository;

import org.poolc.api.officialactivity.domain.OfficialActivityQrAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OfficialActivityQrAttendanceRepository extends JpaRepository<OfficialActivityQrAttendance, Long> {
    Optional<OfficialActivityQrAttendance> findByOfficialActivityIdAndMemberLoginId(Long activityId, String memberLoginId);
    List<OfficialActivityQrAttendance> findByOfficialActivityIdIn(List<Long> activityIds);

    @Modifying
    @Query(value = "INSERT INTO official_activity_qr_attendance (id, activity_id, member_login_id, checked_in_at) "
            + "VALUES (nextval('official_activity_qr_attendance_seq'), :activityId, :memberLoginId, CURRENT_TIMESTAMP) "
            + "ON CONFLICT (activity_id, member_login_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("activityId") Long activityId, @Param("memberLoginId") String memberLoginId);
}
