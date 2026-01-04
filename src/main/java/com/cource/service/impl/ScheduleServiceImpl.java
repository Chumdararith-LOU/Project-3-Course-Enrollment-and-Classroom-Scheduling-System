package com.cource.service.impl;

import com.cource.dto.schedule.ScheduleRequestDTO;
import com.cource.dto.schedule.ScheduleResponseDTO;
import com.cource.entity.*;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.exception.UnauthorizedException;
import com.cource.repository.*;
import com.cource.service.ScheduleService;
import com.cource.util.TimeConflictChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {
    private final ClassScheduleRepository classScheduleRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final CourseLecturerRepository courseLecturerRepository;
    private final TimeConflictChecker timeConflictChecker;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    @PreAuthorize("hasRole('LECTURER')")
    public ScheduleResponseDTO createSchedule(ScheduleRequestDTO dto, String lecturerEmail) {
        User lecturer = userRepository.findByEmail(lecturerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found"));

        CourseOffering offering = courseOfferingRepository.findById(dto.getOfferingId())
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found"));

        boolean isAssigned = courseLecturerRepository.findByOfferingIdAndPrimaryTrue(offering.getId())
                .stream()
                .anyMatch(cl -> cl.getLecturer().getId().equals(lecturer.getId()));

        if (!isAssigned) {
            throw new UnauthorizedException("You are not assigned to this course offering.");
        }

        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (room.getCapacity() < offering.getCapacity()) {
            throw new ConflictException("Room capacity (" + room.getCapacity() +
                    ") is too small for course capacity (" + offering.getCapacity() + ")");
        }

        ClassSchedule newSchedule = new ClassSchedule();
        newSchedule.setDayOfWeek(dto.getDayOfWeek());
        newSchedule.setStartTime(dto.getStartTime());
        newSchedule.setEndTime(dto.getEndTime());

        List<ClassSchedule> existingRoomSchedules = classScheduleRepository.findByRoomId(room.getId());
        if (timeConflictChecker.hasConflict(newSchedule, existingRoomSchedules)) {
            throw new ConflictException("Room " + room.getRoomNumber() + " is already booked at this time.");
        }

        newSchedule.setOffering(offering);
        newSchedule.setRoom(room);
        ClassSchedule saved = classScheduleRepository.save(newSchedule);

        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('LECTURER')")
    public List<ScheduleResponseDTO> getSchedulesByLecturer(String lecturerEmail) {
        User lecturer = userRepository.findByEmail(lecturerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found"));

        List<CourseLecturer> assignments = courseLecturerRepository.findByLecturerId(lecturer.getId());
        List<Long> offeringIds = assignments.stream()
                .map(cl -> cl.getOffering().getId())
                .collect(Collectors.toList());

        List<ClassSchedule> schedules = classScheduleRepository.findByOfferingIdIn(offeringIds);

        return schedules.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponseDTO> getStudentSchedule(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndStatus(studentId, "ENROLLED");

        if (enrollments.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> offeringIds = enrollments.stream()
                .map(e -> e.getOffering().getId())
                .collect(Collectors.toList());

        List<ClassSchedule> schedules = classScheduleRepository.findByOfferingIdIn(offeringIds);

        return schedules.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private ScheduleResponseDTO mapToDTO(ClassSchedule schedule) {
        ScheduleResponseDTO dto = new ScheduleResponseDTO();
        dto.setId(schedule.getId());
        dto.setCourseName(schedule.getOffering().getCourse().getTitle());
        dto.setCourseCode(schedule.getOffering().getCourse().getCourseCode());
        dto.setRoomNumber(schedule.getRoom().getRoomNumber());
        dto.setDayOfWeek(schedule.getDayOfWeek());
        dto.setStartTime(schedule.getStartTime());
        dto.setEndTime(schedule.getEndTime());
        return dto;
    }
}
