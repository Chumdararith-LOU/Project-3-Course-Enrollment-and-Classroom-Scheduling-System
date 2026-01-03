package com.cource.dto.course;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AcademicTermRequestDTO {
    @NotBlank(message = "Term code is required")
    private String termCode;

    @NotBlank(message = "Term name is required")
    private String termName;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean isActive;
}
