package com.cource.dto.lecturer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerDashboardDTO {
    private long activeCourses;
    private long totalStudents;
    private long upcomingClasses;
    private double averageAttendanceRate;
}
