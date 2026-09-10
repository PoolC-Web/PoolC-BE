package org.poolc.api.officialactivity.domain;

import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "official_activity_qr_attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "member_login_id"}))
@SequenceGenerator(name = "OFFICIAL_ACTIVITY_QR_ATTENDANCE_SEQ", sequenceName = "OFFICIAL_ACTIVITY_QR_ATTENDANCE_SEQ")
public class OfficialActivityQrAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "OFFICIAL_ACTIVITY_QR_ATTENDANCE_SEQ")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "activity_id", nullable = false)
    private OfficialActivity officialActivity;

    @Column(name = "member_login_id", nullable = false, length = 40)
    private String memberLoginId;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    protected OfficialActivityQrAttendance() {
    }

    public OfficialActivityQrAttendance(OfficialActivity officialActivity, String memberLoginId) {
        this.officialActivity = officialActivity;
        this.memberLoginId = memberLoginId;
        this.checkedInAt = LocalDateTime.now();
    }
}
