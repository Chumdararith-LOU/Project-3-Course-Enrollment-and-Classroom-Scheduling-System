package com.cource.dto.course;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicTermResponseDTO {
    private Long id;
    private String termCode;
    private String termName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;

}
