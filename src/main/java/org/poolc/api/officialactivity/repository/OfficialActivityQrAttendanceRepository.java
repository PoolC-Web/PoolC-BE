package org.poolc.api.officialactivity.repository;

import org.poolc.api.officialactivity.domain.OfficialActivityQrAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfficialActivityQrAttendanceRepository extends JpaRepository<OfficialActivityQrAttendance, Long> {
    Optional<OfficialActivityQrAttendance> findByOfficialActivityIdAndMemberLoginId(Long activityId, String memberLoginId);
    List<OfficialActivityQrAttendance> findByOfficialActivityIdIn(List<Long> activityIds);
}
