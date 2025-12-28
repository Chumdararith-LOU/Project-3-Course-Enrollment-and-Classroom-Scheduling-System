package com.cource.service;

import com.cource.dto.schedule.ScheduleRequestDTO;
import com.cource.dto.schedule.ScheduleResponseDTO;

import java.util.List;

public interface ScheduleService {
    ScheduleResponseDTO createSchedule(ScheduleRequestDTO dto, String lecturerEmail);
    List<ScheduleResponseDTO> getSchedulesByLecturer(String lecturerEmail);
}
