package com.cource.service.impl;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.attendance.AttendanceResponseDTO;
import com.cource.entity.*;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.exception.UnauthorizedException;
import com.cource.repository.*;
import com.cource.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseLecturerRepository courseLecturerRepository;

    @Override
    @Transactional
    @PreAuthorize("hasRole('LECTURER')")
    public AttendanceResponseDTO markAttendance(AttendanceRequestDTO dto, String lecturerEmail) {
        User lecturer = userRepository.findByEmail(lecturerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found"));

        ClassSchedule schedule = classScheduleRepository.findById(dto.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        validateLecturerAccess(lecturer, schedule.getOffering());

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(dto.getStudentId(), schedule.getOffering().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student is not enrolled in this course"));

        boolean exists = attendanceRepository.existsByStudentIdAndScheduleId(
                dto.getStudentId(),
                dto.getScheduleId(),
                enrollment.getId(),
                dto.getAttendanceDate()
        );

        if (exists) {
            throw new ConflictException("Attendance already recorded for this student on this date.");
        }

        Attendance attendance = new Attendance();
        attendance.setEnrollment(enrollment);
        attendance.setSchedule(schedule);
        attendance.setAttendanceDate(dto.getAttendanceDate());
        attendance.setStatus(dto.getStatus());
        attendance.setNotes(dto.getNotes());
        attendance.setRecordedBy(lecturer);

        Attendance saved = attendanceRepository.save(attendance);
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('LECTURER')")
    public List<AttendanceResponseDTO> getAttendanceBySchedule(Long scheduleId, String lecturerEmail) {
        User lecturer = userRepository.findByEmail(lecturerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found"));

        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        validateLecturerAccess(lecturer, schedule.getOffering());

        List<Attendance> records = attendanceRepository.findByScheduleId(scheduleId);
        return records.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private void validateLecturerAccess(User lecturer, CourseOffering offering) {
        boolean isAssigned = courseLecturerRepository.findByOfferingIdAndPrimaryTrue(offering.getId())
                .stream()
                .anyMatch(cl -> cl.getLecturer().getId().equals(lecturer.getId()));

        if (!isAssigned) {
            throw new UnauthorizedException("You are not authorized to manage attendance for this course.");
        }
    }

    private AttendanceResponseDTO mapToDTO(Attendance attendance) {
        AttendanceResponseDTO dto = new AttendanceResponseDTO();
        dto.setId(attendance.getId());

        Student student = attendance.getEnrollment().getStudent();
        dto.setStudentId(student.getId());
        dto.setStudentName(student.getFirstName() + " " + student.getLastName());
        dto.setStudentIdCard(student.getStudentId());

        dto.setStatus(attendance.getStatus());
        dto.setDate(attendance.getAttendanceDate());
        dto.setNotes(attendance.getNotes());
        return dto;
    }
}
