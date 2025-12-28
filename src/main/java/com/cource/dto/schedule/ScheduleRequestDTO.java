package com.cource.dto.schedule;

import java.time.LocalTime;

public class ScheduleRequestDTO {
    private Long offeringId;
    private Long roomId;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}
