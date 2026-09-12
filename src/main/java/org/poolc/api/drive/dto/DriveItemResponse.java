package org.poolc.api.drive.dto;
import lombok.Value; import org.poolc.api.drive.domain.DriveItem;
import java.time.LocalDateTime; import java.util.List;
@Value public class DriveItemResponse { Long id; Long parentId; String type; String name; Long size; String contentType; String ownerLoginId; List<String> sharedMemberLoginIds; LocalDateTime updatedAt;
 public static DriveItemResponse of(DriveItem item) { return new DriveItemResponse(item.getId(), item.getParent()==null?null:item.getParent().getId(), item.getType().name(), item.getName(), item.getSize(), item.getContentType(), item.getOwner().getLoginID(), item.getSharedMemberLoginIds(), item.getUpdatedAt()); }}
