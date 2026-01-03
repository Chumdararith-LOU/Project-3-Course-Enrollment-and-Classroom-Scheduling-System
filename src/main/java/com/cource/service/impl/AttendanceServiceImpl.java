package com.cource.service.impl;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.attendance.AttendanceResponseDTO;
import com.cource.dto.attendance.AttendanceSummaryDTO;
import com.cource.entity.*;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.*;
import com.cource.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Attendance recordAttendance(AttendanceRequestDTO request) {
        Attendance attendance = new Attendance();
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(request.getStatus());
        attendance.setNotes(request.getNotes());
        return attendanceRepository.save(attendance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getAttendanceBySchedule(Long scheduleId) {
        List<Attendance> records = attendanceRepository.findByScheduleId(scheduleId);
        return records.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getAttendanceByScheduleAndDate(Long scheduleId, LocalDate date) {
        List<Attendance> records = attendanceRepository.findByScheduleId(scheduleId);
        return records.stream()
                .filter(a -> a.getAttendanceDate().equals(date))
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getStudentAttendance(Long studentId, Long offeringId) {
        List<Attendance> records = attendanceRepository.findByStudentIdAndOfferingId(studentId, offeringId);
        return records.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryDTO getStudentAttendanceSummary(Long studentId, Long offeringId) {
        List<Attendance> records = attendanceRepository.findByStudentIdAndOfferingId(studentId, offeringId);
        long presentCount = records.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long totalSessions = records.size();
        double percentage = totalSessions == 0 ? 0.0 : (double) presentCount / totalSessions * 100;
        
        return new AttendanceSummaryDTO("PRESENT", presentCount, percentage);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getScheduleAttendanceStats(Long scheduleId) {
        List<Attendance> records = attendanceRepository.findByScheduleId(scheduleId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", records.size());
        stats.put("present", records.stream().filter(a -> "PRESENT".equals(a.getStatus())).count());
        stats.put("late", records.stream().filter(a -> "LATE".equals(a.getStatus())).count());
        stats.put("absent", records.stream().filter(a -> "ABSENT".equals(a.getStatus())).count());
        return stats;
    }

    @Override
    @Transactional
    public Attendance updateAttendance(Long attendanceId, AttendanceRequestDTO request) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));
        attendance.setStatus(request.getStatus());
        attendance.setNotes(request.getNotes());
        return attendanceRepository.save(attendance);
    }

    @Override
    @Transactional
    public void deleteAttendance(Long attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new ResourceNotFoundException("Attendance record not found");
        }
        attendanceRepository.deleteById(attendanceId);
    }

    @Override
    @Transactional
    public List<Attendance> bulkRecordAttendance(Long scheduleId, LocalDate date, List<Long> studentIds, String status, Long recordedBy) {
        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        User recorder = userRepository.findById(recordedBy)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<Attendance> attendances = new java.util.ArrayList<>();
        for (Long studentId : studentIds) {
            Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(studentId, schedule.getOffering().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student enrollment not found"));
            
            Attendance attendance = new Attendance();
            attendance.setEnrollment(enrollment);
            attendance.setSchedule(schedule);
            attendance.setAttendanceDate(date);
            attendance.setStatus(status);
            attendance.setRecordedBy(recorder);
            attendances.add(attendanceRepository.save(attendance));
        }
        return attendances;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassSchedule> getTodaySchedulesForLecturer(Long lecturerId) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean attendanceExists(Long studentId, Long scheduleId, LocalDate date) {
        List<Attendance> records = attendanceRepository.findByScheduleId(scheduleId);
        return records.stream()
                .anyMatch(a -> a.getAttendanceDate().equals(date));
    }

    @Override
    @Transactional(readOnly = true)
    public double getAttendanceRate(Long studentId, Long offeringId) {
        List<Attendance> records = attendanceRepository.findByStudentIdAndOfferingId(studentId, offeringId);
        if (records.isEmpty()) return 0.0;
        long presentCount = records.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        return (double) presentCount / records.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getOfferingAttendance(Long offeringId, LocalDate fromDate, LocalDate toDate) {
        // Placeholder - need a custom query in repository
        return List.of();
    }

    private AttendanceResponseDTO mapToDTO(Attendance attendance) {
        AttendanceResponseDTO dto = new AttendanceResponseDTO();
        dto.setId(attendance.getId());

        if (attendance.getEnrollment() != null && attendance.getEnrollment().getStudent() != null) {
            Student student = attendance.getEnrollment().getStudent();
            dto.setStudentId(student.getId());
            dto.setStudentName(student.getFirstName() + " " + student.getLastName());
            dto.setStudentIdCard(student.getStudentId());
        }

        dto.setStatus(attendance.getStatus());
        dto.setDate(attendance.getAttendanceDate());
        dto.setNotes(attendance.getNotes());
        return dto;
    }
}
