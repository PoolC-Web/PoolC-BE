package org.poolc.api.member.repository;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class RecognizedProjectHours {
    Long projectId;
    String title;
    BigDecimal hours;
}
