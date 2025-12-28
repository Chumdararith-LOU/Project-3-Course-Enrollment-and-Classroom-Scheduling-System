package com.cource.dto.schedule;

import java.time.LocalTime;

public class ScheduleResponseDTO {
    private Long id;
    private String courseName;
    private String courseCode;
    private String roomNumber;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String lecturerName;
}
