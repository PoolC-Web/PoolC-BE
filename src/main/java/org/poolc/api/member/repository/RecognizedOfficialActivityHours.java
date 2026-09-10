package org.poolc.api.member.repository;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class RecognizedOfficialActivityHours {
    Long activityId;
    String title;
    BigDecimal hours;
}
