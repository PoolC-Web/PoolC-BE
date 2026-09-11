package org.poolc.api.officialactivity.dto;

import lombok.Getter;
import org.poolc.api.officialactivity.domain.OfficialActivity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
public class OfficialActivityResponse {
    private final Long id;
    private final List<String> memberLoginIds;
    private final List<String> memberNames;
    private final List<OfficialActivityParticipantResponse> participants;
    private final LocalDate activityDate;
    private final String title;
    private final BigDecimal recognizedHours;
    private final boolean qrEnabled;

    public OfficialActivityResponse(OfficialActivity activity, List<String> memberNames, List<OfficialActivityParticipantResponse> participants) {
        this.id = activity.getId();
        this.memberLoginIds = activity.getMemberLoginIds();
        this.memberNames = memberNames;
        this.participants = participants;
        this.activityDate = activity.getActivityDate();
        this.title = activity.getTitle();
        this.recognizedHours = activity.getRecognizedHours();
        this.qrEnabled = activity.isQrEnabled();
    }
}
