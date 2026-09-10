package org.poolc.api.officialactivity.dto;

import lombok.Getter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
public class CreateOfficialActivityRequest {
    @NotEmpty
    private List<@NotBlank String> memberLoginIds;

    @NotNull
    private LocalDate activityDate;

    @NotBlank
    private String title;

    @NotNull
    @DecimalMin(value = "0.1")
    private BigDecimal recognizedHours;
}
