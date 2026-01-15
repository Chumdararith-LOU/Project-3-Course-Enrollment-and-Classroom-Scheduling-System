package com.cource.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.cource.dto.attendance.AttendanceCodeDetailsDTO;
import com.cource.dto.attendance.AttendanceCodeEnterResultDTO;
import com.cource.entity.Attendance;
import com.cource.entity.User;
import com.cource.repository.AttendanceRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceCodeApplicationService {

    private final AttendanceCodeService attendanceCodeService;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;

    public AttendanceCodeDetailsDTO generateDetails(Long scheduleId, Long lecturerId, Integer presentMinutes,
            Integer lateMinutes, Long issuedAt) {
        var info = attendanceCodeService.generate(scheduleId, lecturerId, presentMinutes, lateMinutes, issuedAt);
        return enrichDetails(scheduleId, info);
    }

    // Overload for backward compatibility
    public AttendanceCodeDetailsDTO generateDetails(Long scheduleId, Long lecturerId, Integer presentMinutes,
            Integer lateMinutes) {
        return generateDetails(scheduleId, lecturerId, presentMinutes, lateMinutes, null);
    }

    public AttendanceCodeDetailsDTO currentDetails(Long scheduleId) {
        var info = attendanceCodeService.get(scheduleId);
        if (info == null) {
            return null;
        }
        return enrichDetails(scheduleId, info);
    }

    public AttendanceCodeEnterResultDTO enterCode(Long scheduleId, String code, Long studentId) {
        if (scheduleId == null || code == null || code.isBlank()) {
            throw new IllegalArgumentException("scheduleId and code required");
        }

        var info = attendanceCodeService.get(scheduleId);
        if (info == null || !info.getCode().equals(code)) {
            throw new SecurityException("Invalid or expired code");
        }

        var schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

        Long offeringId = schedule.getOffering() == null ? null : schedule.getOffering().getId();
        if (offeringId == null) {
            throw new IllegalArgumentException("Offering not found for schedule");
        }

        var enrollment = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId)
                .orElseThrow(() -> new SecurityException("Student not enrolled in offering"));

        LocalDate date = LocalDate.now();
        boolean exists = attendanceRepository.existsByStudentIdAndScheduleId(studentId, scheduleId, enrollment.getId(),
                date);
        if (exists) {
            return new AttendanceCodeEnterResultDTO(false, true, null, null);
        }

        // compute status by time windows relative to schedule.startTime
        LocalTime start = schedule.getStartTime();
        LocalTime now = LocalTime.now(ZoneId.systemDefault());

        Integer presentWindow = info.getPresentWindowMinutes() != null ? info.getPresentWindowMinutes() : 15;
        Integer lateWindow = info.getLateWindowMinutes() != null ? info.getLateWindowMinutes() : 30;

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

        Attendance a = new Attendance();
        a.setEnrollment(enrollment);
        a.setSchedule(schedule);
        a.setAttendanceDate(date);
        a.setStatus(status);
        User me = new User();
        me.setId(studentId);
        a.setRecordedBy(me);

        var saved = attendanceRepository.save(a);
        return new AttendanceCodeEnterResultDTO(true, false, saved.getId(), status);
    }

    private AttendanceCodeDetailsDTO enrichDetails(Long scheduleId, AttendanceCodeService.CodeInfo info) {
        Long offeringId = null;
        long enrolledCount = 0;

        var schedOpt = classScheduleRepository.findById(scheduleId);
        if (schedOpt.isPresent()) {
            var sched = schedOpt.get();
            if (sched.getOffering() != null) {
                offeringId = sched.getOffering().getId();
                enrolledCount = enrollmentRepository.countByOfferingIdAndStatus(offeringId, "ENROLLED");
            }
        }

        return new AttendanceCodeDetailsDTO(
                info.getCode(),
                info.getIssuedAt(),
                info.getPresentWindowMinutes(),
                info.getLateWindowMinutes(),
                offeringId,
                enrolledCount);
    }
}
