package org.poolc.api.member.repository;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class RecognizedActivityHours {
    Long activityId;
    String title;
    BigDecimal hours;
    boolean hosted;
}
