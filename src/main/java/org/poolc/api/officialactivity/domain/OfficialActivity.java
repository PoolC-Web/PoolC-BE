package org.poolc.api.officialactivity.domain;

import lombok.Getter;
import org.poolc.api.common.domain.TimestampEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "official_activity")
@SequenceGenerator(name = "OFFICIAL_ACTIVITY_SEQ", sequenceName = "OFFICIAL_ACTIVITY_SEQ")
public class OfficialActivity extends TimestampEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "OFFICIAL_ACTIVITY_SEQ")
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "official_activity_members", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "member_login_id", nullable = false, length = 40)
    private List<String> memberLoginIds = new ArrayList<>();

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "recognized_hours", nullable = false, precision = 6, scale = 1)
    private BigDecimal recognizedHours;

    @Column(name = "qr_enabled", nullable = false)
    private boolean qrEnabled;

    protected OfficialActivity() {
    }

    public OfficialActivity(List<String> memberLoginIds, LocalDate activityDate, String title, BigDecimal recognizedHours) {
        this.memberLoginIds = new ArrayList<>(memberLoginIds);
        this.activityDate = activityDate;
        this.title = title;
        this.recognizedHours = recognizedHours;
    }

    public void update(List<String> memberLoginIds, LocalDate activityDate, String title, BigDecimal recognizedHours) {
        this.memberLoginIds.clear();
        this.memberLoginIds.addAll(memberLoginIds);
        this.activityDate = activityDate;
        this.title = title;
        this.recognizedHours = recognizedHours;
    }

    public void addMemberLoginId(String memberLoginId) {
        if (!memberLoginIds.contains(memberLoginId)) {
            memberLoginIds.add(memberLoginId);
        }
    }

    public void enableQr() {
        this.qrEnabled = true;
    }

    public void disableQr() {
        this.qrEnabled = false;
    }
}
