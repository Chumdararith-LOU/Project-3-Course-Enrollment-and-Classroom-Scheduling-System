package com.cource.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.cource.entity.*;
import org.springframework.stereotype.Service;

import com.cource.dto.attendance.AttendanceCodeDetailsDTO;
import com.cource.dto.attendance.AttendanceCodeEnterResultDTO;
import com.cource.repository.AttendanceRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceCodeApplicationService {

    private final AttendanceCodeService attendanceCodeService;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;

    @Transactional
    public AttendanceCodeDetailsDTO generateDetails(Long scheduleId, Long lecturerId, Integer presentMinutes, Integer lateMinutes) {
        AttendanceCode code = attendanceCodeService.generateAttendanceCode(scheduleId, lecturerId);

        boolean changed = false;
        if (presentMinutes != null && !presentMinutes.equals(code.getPresentWindowMinutes())) {
            code.setPresentWindowMinutes(presentMinutes);
            changed = true;
        }
        if (lateMinutes != null && !lateMinutes.equals(code.getLateWindowMinutes())) {
            code.setLateWindowMinutes(lateMinutes);
            changed = true;
        }
        if (changed) {
            attendanceCodeService.save(code);
        }

        return enrichDetails(scheduleId, code);
    }

    public AttendanceCodeDetailsDTO currentDetails(Long scheduleId) {
        AttendanceCode code = attendanceCodeService.findByScheduleId(scheduleId);
        return enrichDetails(scheduleId, code);
    }

    public AttendanceCodeEnterResultDTO enterCode(Long scheduleId, String code, Long studentId) {
        if (scheduleId == null || code == null || code.isBlank()) {
            throw new IllegalArgumentException("scheduleId and code required");
        }

        AttendanceCode ac = attendanceCodeService.findByCode(code);
        if (ac == null || !ac.getSchedule().getId().equals(scheduleId)) {
            throw new SecurityException("Invalid or expired code");
        }

        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        Long offeringId = schedule.getOffering().getId();

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId)
                .orElseThrow(() -> new SecurityException("Student not enrolled in offering"));

        LocalDate date = LocalDate.now();
        boolean exists = attendanceRepository.existsByStudentIdAndScheduleId(studentId, scheduleId, enrollment.getId(), date);
        if (exists) {
            return new AttendanceCodeEnterResultDTO(false, true, null, null);
        }

        LocalTime start = schedule.getStartTime();
        LocalTime now = LocalTime.now(ZoneId.systemDefault());
        Integer presentWindow = ac.getPresentWindowMinutes() != null ? ac.getPresentWindowMinutes() : 15;
        Integer lateWindow = ac.getLateWindowMinutes() != null ? ac.getLateWindowMinutes() : 30;
        String status = "PRESENT";

        if (start != null) {
            LocalTime presentCut = start.plusMinutes(presentWindow);
            LocalTime lateCut = start.plusMinutes(lateWindow);
            if (now.isAfter(presentCut) && !now.isAfter(lateCut)) {
                status = "LATE";
            } else if (now.isAfter(lateCut)) {
                status = "ABSENT";
            }
        }

        Attendance attendance = new Attendance();
        attendance.setEnrollment(enrollment);
        attendance.setSchedule(schedule);
        attendance.setAttendanceDate(date);
        attendance.setStatus(status);
        User student = new User();
        student.setId(studentId);
        attendance.setRecordedBy(student);

        Attendance saved = attendanceRepository.save(attendance);
        return new AttendanceCodeEnterResultDTO(true, false, saved.getId(), status);
    }

    private AttendanceCodeDetailsDTO enrichDetails(Long scheduleId, AttendanceCode code) {
        if (code == null) return null;

        Long offeringId = null;
        long enrolledCount = 0;
        ClassSchedule schedule = classScheduleRepository.findById(scheduleId).orElse(null);
        if (schedule != null && schedule.getOffering() != null) {
            offeringId = schedule.getOffering().getId();
            enrolledCount = enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ENROLLED");
        }

        return new AttendanceCodeDetailsDTO(
                code.getCode(),
                code.getIssuedAt(),
                code.getPresentWindowMinutes(),
                code.getLateWindowMinutes(),
                offeringId,
                enrolledCount
        );
    }
}
