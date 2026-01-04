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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final UserRepository userRepository;
    private final CourseLecturerRepository courseLecturerRepository;

    @Override
    public Attendance recordAttendance(AttendanceRequestDTO request) {
        // Get the schedule
        ClassSchedule schedule = classScheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        // Get the enrollment for this student and offering
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(
                request.getStudentId(), schedule.getOffering().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found for student"));

        // Check if attendance already exists
        boolean exists = attendanceRepository.existsByStudentIdAndScheduleId(
                request.getStudentId(), request.getScheduleId(), enrollment.getId(), request.getAttendanceDate());
        if (exists) {
            throw new IllegalArgumentException("Attendance already recorded for this student on this date");
        }

        // Create attendance record
        Attendance attendance = new Attendance();
        attendance.setEnrollment(enrollment);
        attendance.setSchedule(schedule);
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(request.getStatus() != null ? request.getStatus() : "PRESENT");
        attendance.setNotes(request.getNotes());

        if (request.getLecturerId() != null) {
            User recorder = new User();
            recorder.setId(request.getLecturerId());
            attendance.setRecordedBy(recorder);
        }

        return attendanceRepository.save(attendance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getAttendanceBySchedule(Long scheduleId) {
        List<Attendance> records = attendanceRepository.findByScheduleIdWithStudent(scheduleId);
        return records.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getAttendanceByScheduleAndDate(Long scheduleId, LocalDate date) {
        List<Attendance> records = attendanceRepository.findByScheduleIdAndDate(scheduleId, date);
        return records.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getStudentAttendance(Long studentId, Long offeringId) {
        List<Attendance> records = attendanceRepository.findByStudentIdAndOfferingId(studentId, offeringId);
        return records.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryDTO getStudentAttendanceSummary(Long studentId, Long offeringId) {
        List<Attendance> records = attendanceRepository.findByStudentIdAndOfferingId(studentId, offeringId);

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        int present = 0, late = 0, absent = 0, excused = 0;
        for (Attendance a : records) {
            switch (a.getStatus().toUpperCase()) {
                case "PRESENT" -> present++;
                case "LATE" -> late++;
                case "ABSENT" -> absent++;
                case "EXCUSED" -> excused++;
            }
        }

        int total = records.size();
        double rate = total > 0 ? ((present + late + excused) * 100.0 / total) : 0;
        String grade = calculateAttendanceGrade(rate);

        return AttendanceSummaryDTO.builder()
                .studentId(studentId)
                .studentName(enrollment.getStudent().getFullName())
                .offeringId(offeringId)
                .courseName(enrollment.getOffering().getCourse().getTitle())
                .courseCode(enrollment.getOffering().getCourse().getCourseCode())
                .totalClasses(total)
                .presentCount(present)
                .lateCount(late)
                .absentCount(absent)
                .excusedCount(excused)
                .attendanceRate(rate)
                .attendanceGrade(grade)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getScheduleAttendanceStats(Long scheduleId) {
        List<Attendance> records = attendanceRepository.findByScheduleId(scheduleId);

        Map<String, Long> statusCounts = records.stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().toUpperCase(), Collectors.counting()));

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", records.size());
        stats.put("present", statusCounts.getOrDefault("PRESENT", 0L));
        stats.put("late", statusCounts.getOrDefault("LATE", 0L));
        stats.put("absent", statusCounts.getOrDefault("ABSENT", 0L));
        stats.put("excused", statusCounts.getOrDefault("EXCUSED", 0L));

        if (!records.isEmpty()) {
            stats.put("attendanceRate",
                    (statusCounts.getOrDefault("PRESENT", 0L) + statusCounts.getOrDefault("LATE", 0L)) * 100.0
                            / records.size());
        } else {
            stats.put("attendanceRate", 0.0);
        }

        return stats;
    }

    @Override
    public Attendance updateAttendance(Long attendanceId, AttendanceRequestDTO request) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            attendance.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            attendance.setNotes(request.getNotes());
        }
        if (request.getAttendanceDate() != null) {
            attendance.setAttendanceDate(request.getAttendanceDate());
        }

        return attendanceRepository.save(attendance);
    }

    @Override
    public void deleteAttendance(Long attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new ResourceNotFoundException("Attendance not found");
        }
        attendanceRepository.deleteById(attendanceId);
    }

    @Override
    public List<Attendance> bulkRecordAttendance(Long scheduleId, LocalDate date, List<Long> studentIds, String status,
            Long recordedBy) {
        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        Long offeringId = schedule.getOffering().getId();
        List<Attendance> results = new ArrayList<>();

        for (Long studentId : studentIds) {
            try {
                Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId)
                        .orElse(null);
                if (enrollment == null)
                    continue;

                boolean exists = attendanceRepository.existsByStudentIdAndScheduleId(
                        studentId, scheduleId, enrollment.getId(), date);
                if (exists)
                    continue;

                Attendance attendance = new Attendance();
                attendance.setEnrollment(enrollment);
                attendance.setSchedule(schedule);
                attendance.setAttendanceDate(date);
                attendance.setStatus(status);

                if (recordedBy != null) {
                    User recorder = new User();
                    recorder.setId(recordedBy);
                    attendance.setRecordedBy(recorder);
                }

                results.add(attendanceRepository.save(attendance));
            } catch (Exception e) {
                // Log and continue with next student
            }
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassSchedule> getTodaySchedulesForLecturer(Long lecturerId) {
        String today = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase();

        // Get all offerings for this lecturer
        var courseLecturers = courseLecturerRepository.findByLecturerId(lecturerId);
        List<Long> offeringIds = courseLecturers.stream()
                .map(cl -> cl.getOffering().getId())
                .collect(Collectors.toList());

        if (offeringIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Get schedules for today
        return classScheduleRepository.findAll().stream()
                .filter(s -> offeringIds.contains(s.getOffering().getId()))
                .filter(s -> s.getDayOfWeek().equalsIgnoreCase(today))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean attendanceExists(Long studentId, Long scheduleId, LocalDate date) {
        return attendanceRepository.existsByStudentScheduleAndDate(studentId, scheduleId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public double getAttendanceRate(Long studentId, Long offeringId) {
        List<Attendance> records = attendanceRepository.findByStudentIdAndOfferingId(studentId, offeringId);
        if (records.isEmpty())
            return 0.0;

        long presentOrLate = records.stream()
                .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()) || "LATE".equalsIgnoreCase(a.getStatus()))
                .count();

        return presentOrLate * 100.0 / records.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getOfferingAttendance(Long offeringId, LocalDate fromDate, LocalDate toDate) {
        List<Attendance> records = attendanceRepository.findByOfferingIdBetweenDates(offeringId, fromDate, toDate);
        return records.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    // Helper methods
    private AttendanceResponseDTO toResponseDTO(Attendance a) {
        AttendanceResponseDTO.AttendanceResponseDTOBuilder builder = AttendanceResponseDTO.builder()
                .id(a.getId())
                .attendanceDate(a.getAttendanceDate())
                .status(a.getStatus())
                .notes(a.getNotes())
                .recordedAt(a.getRecordedAt());

        if (a.getEnrollment() != null) {
            User student = a.getEnrollment().getStudent();
            if (student != null) {
                builder.studentId(student.getId())
                        .studentName(student.getFullName())
                        .studentEmail(student.getEmail());
            }
            if (a.getEnrollment().getOffering() != null) {
                builder.offeringId(a.getEnrollment().getOffering().getId());
                if (a.getEnrollment().getOffering().getCourse() != null) {
                    builder.courseName(a.getEnrollment().getOffering().getCourse().getTitle())
                            .courseCode(a.getEnrollment().getOffering().getCourse().getCourseCode());
                }
            }
        }

        if (a.getSchedule() != null) {
            builder.scheduleId(a.getSchedule().getId())
                    .scheduleDayOfWeek(a.getSchedule().getDayOfWeek())
                    .scheduleTime(a.getSchedule().getStartTime() + " - " + a.getSchedule().getEndTime());
        }

        if (a.getRecordedBy() != null) {
            builder.recordedById(a.getRecordedBy().getId())
                    .recordedByName(a.getRecordedBy().getFullName());
        }

        return builder.build();
    }

    private String calculateAttendanceGrade(double rate) {
        if (rate >= 90)
            return "A";
        if (rate >= 80)
            return "B";
        if (rate >= 70)
            return "C";
        if (rate >= 60)
            return "D";
        return "F";
    }
}
