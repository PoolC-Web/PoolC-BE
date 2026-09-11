package org.poolc.api.officialactivity.repository;

import org.poolc.api.officialactivity.domain.OfficialActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfficialActivityRepository extends JpaRepository<OfficialActivity, Long> {
    @Modifying
    @Query(value = "INSERT INTO official_activity_members (activity_id, member_login_id) "
            + "VALUES (:activityId, :memberLoginId) "
            + "ON CONFLICT (activity_id, member_login_id) DO NOTHING", nativeQuery = true)
    int insertMemberIfAbsent(@Param("activityId") Long activityId, @Param("memberLoginId") String memberLoginId);
}
