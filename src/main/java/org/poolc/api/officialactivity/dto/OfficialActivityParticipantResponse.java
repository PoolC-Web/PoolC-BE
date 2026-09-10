package org.poolc.api.officialactivity.dto;

import lombok.Value;
import org.poolc.api.officialactivity.domain.OfficialActivityParticipantSource;

@Value
public class OfficialActivityParticipantResponse {
    String loginId;
    String name;
    String department;
    String studentId;
    String phoneNumber;
    OfficialActivityParticipantSource source;
}
