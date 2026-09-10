package org.poolc.api.officialactivity.domain;

import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "official_activity_qr_token")
public class OfficialActivityQrToken {
    @Id
    @Column(length = 64)
    private String token;

    @ManyToOne
    @JoinColumn(name = "activity_id", nullable = false)
    private OfficialActivity officialActivity;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected OfficialActivityQrToken() {
    }

    public OfficialActivityQrToken(String token, OfficialActivity officialActivity, LocalDateTime expiresAt) {
        this.token = token;
        this.officialActivity = officialActivity;
        this.expiresAt = expiresAt;
    }
}
