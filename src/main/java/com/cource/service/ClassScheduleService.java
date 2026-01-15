package com.cource.service;

import com.cource.dto.schedule.ClassScheduleDTO;
import com.cource.entity.ClassSchedule;

import java.util.List;

public interface ClassScheduleService {
    List<ClassScheduleDTO> getAll(Long offeringId);

    ClassScheduleDTO getById(Long id);
    ClassScheduleDTO create(ClassSchedule schedule);
    ClassScheduleDTO update(Long id, ClassSchedule schedule);

    void delete(Long id);

    ClassScheduleDTO createFromParams(
            Long lecturerId,
            Long offeringId,
            String dayOfWeek,
            String startTime,
            String endTime,
            String roomNumber,
            String building,
            String roomType);

    ClassScheduleDTO updateFromParams(
            Long scheduleId,
            Long lecturerId,
            Long offeringId,
            String dayOfWeek,
            String startTime,
            String endTime,
            String roomNumber,
            String building,
            String roomType);
}
