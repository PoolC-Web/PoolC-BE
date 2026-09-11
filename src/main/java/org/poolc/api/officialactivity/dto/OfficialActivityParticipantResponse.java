package org.poolc.api.officialactivity.dto;

import lombok.Value;
import org.poolc.api.officialactivity.domain.OfficialActivityParticipantSource;

import java.time.LocalDateTime;

@Value
public class OfficialActivityParticipantResponse {
    String loginId;
    String name;
    String department;
    String studentId;
    String phoneNumber;
    OfficialActivityParticipantSource source;
    LocalDateTime attendedAt;
}
