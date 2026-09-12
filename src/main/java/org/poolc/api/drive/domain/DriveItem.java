package org.poolc.api.drive.domain;

import lombok.Getter;
import org.poolc.api.common.domain.TimestampEntity;
import org.poolc.api.member.domain.Member;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SequenceGenerator(name = "DRIVE_ITEM_SEQ", sequenceName = "DRIVE_ITEM_SEQ")
public class DriveItem extends TimestampEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DRIVE_ITEM_SEQ") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_uuid", nullable = false) private Member owner;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") private DriveItem parent;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private DriveItemType type;
    @Column(nullable = false, length = 255) private String name;
    @Column(length = 64) private String storageId;
    private Long size;
    @Column(length = 255) private String contentType;
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "drive_item_shares", joinColumns = @JoinColumn(name = "drive_item_id"))
    @Column(name = "member_login_id", length = 40) private List<String> sharedMemberLoginIds = new ArrayList<>();
    protected DriveItem() {}
    public DriveItem(Member owner, DriveItem parent, String name) { this.owner = owner; this.parent = parent; this.name = name; this.type = DriveItemType.FOLDER; }
    public DriveItem(Member owner, DriveItem parent, String name, String storageId, long size, String contentType) { this.owner = owner; this.parent = parent; this.name = name; this.type = DriveItemType.FILE; this.storageId = storageId; this.size = size; this.contentType = contentType; }
    public boolean canAccess(Member member) { return member.isAdmin() || owner.getLoginID().equals(member.getLoginID()) || sharedMemberLoginIds.contains(member.getLoginID()); }
    public boolean canManage(Member member) { return member.isAdmin() || owner.getLoginID().equals(member.getLoginID()); }
    public void rename(String name) { this.name = name; }
    public void shareWith(List<String> ids) { this.sharedMemberLoginIds = new ArrayList<>(ids); }
}
