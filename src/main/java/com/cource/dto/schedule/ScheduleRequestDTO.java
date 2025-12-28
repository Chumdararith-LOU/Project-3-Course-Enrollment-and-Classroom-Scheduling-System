package com.cource.dto.schedule;


import lombok.Data;

import java.time.LocalTime;

@Data
public class ScheduleRequestDTO {
    private Long offeringId;
    private Long roomId;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}
