package org.poolc.api.drive.service;

import lombok.RequiredArgsConstructor; import org.poolc.api.drive.domain.*; import org.poolc.api.drive.dto.DriveItemResponse; import org.poolc.api.drive.repository.DriveItemRepository; import org.poolc.api.file.service.FileStorage; import org.poolc.api.member.domain.Member; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.multipart.MultipartFile;
import java.io.IOException; import java.util.*; import java.util.stream.Collectors;
@Service @RequiredArgsConstructor @Transactional
public class DriveService {
 private final DriveItemRepository repository; private final FileStorage fileStorage;
 @Transactional(readOnly=true) public List<DriveItemResponse> list(Member member, Long parentId) { List<DriveItem> items=parentId==null?repository.findByParentIsNullOrderByTypeAscNameAsc():repository.findByParent_IdOrderByTypeAscNameAsc(parentId); return items.stream().filter(i->i.canAccess(member)).map(DriveItemResponse::of).collect(Collectors.toList()); }
 public DriveItemResponse createFolder(Member member, Long parentId, String name) { DriveItem parent=parent(parentId,member); return DriveItemResponse.of(repository.save(new DriveItem(member,parent,validName(name)))); }
 public DriveItemResponse upload(Member member, Long parentId, MultipartFile file) throws IOException { DriveItem parent=parent(parentId,member); String id=UUID.randomUUID().toString(); fileStorage.store(id,file.getInputStream(),file.getSize(),file.getContentType()); return DriveItemResponse.of(repository.save(new DriveItem(member,parent,validName(file.getOriginalFilename()),id,file.getSize(),file.getContentType()))); }
 @Transactional(readOnly=true) public DriveItem item(Member member,Long id){ DriveItem item=repository.findById(id).orElseThrow(()->new IllegalArgumentException("파일을 찾을 수 없습니다.")); if(!item.canAccess(member)) throw new SecurityException("파일 접근 권한이 없습니다."); return item; }
 public DriveItemResponse share(Member member,Long id,List<String> ids){ DriveItem item=item(member,id); if(!item.canManage(member)) throw new SecurityException("공유 권한이 없습니다."); item.shareWith(ids==null?Collections.emptyList():ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList())); return DriveItemResponse.of(item); }
 private DriveItem parent(Long id,Member member){ if(id==null)return null; DriveItem parent=item(member,id); if(parent.getType()!=DriveItemType.FOLDER||!parent.canManage(member))throw new SecurityException("폴더에 업로드할 권한이 없습니다."); return parent; }
 private String validName(String value){ if(value==null||value.trim().isEmpty()||value.contains("/")||value.contains("\\"))throw new IllegalArgumentException("올바른 이름을 입력하세요."); return value.trim(); }
}
