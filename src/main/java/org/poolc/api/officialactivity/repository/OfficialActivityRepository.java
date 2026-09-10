package org.poolc.api.officialactivity.repository;

import org.poolc.api.officialactivity.domain.OfficialActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialActivityRepository extends JpaRepository<OfficialActivity, Long> {
}
