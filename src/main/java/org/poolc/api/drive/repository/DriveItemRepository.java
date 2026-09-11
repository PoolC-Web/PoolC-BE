package org.poolc.api.drive.repository;

import org.poolc.api.drive.domain.DriveItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DriveItemRepository extends JpaRepository<DriveItem, Long> {
    List<DriveItem> findByParent_IdOrderByTypeAscNameAsc(Long parentId);
    List<DriveItem> findByParentIsNullOrderByTypeAscNameAsc();
}
