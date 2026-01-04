package com.cource.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import com.cource.entity.Attendance;
import com.cource.entity.User;
import com.cource.repository.AttendanceRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.EnrollmentRepository;
import com.cource.service.AttendanceCodeService;
import com.cource.util.SecurityHelper;

@RestController
@RequestMapping("/api/attendance-code")
@RequiredArgsConstructor
public class AttendanceCodeController {
    private final AttendanceCodeService attendanceCodeService;
    private final SecurityHelper securityHelper;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;

    // generate a code for a schedule
    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestParam Long scheduleId,
            @RequestParam(required = false) Integer presentMinutes,
            @RequestParam(required = false) Integer lateMinutes) {
        Long lecturerId = securityHelper.getCurrentUserId();
        var info = attendanceCodeService.generate(scheduleId, lecturerId, presentMinutes, lateMinutes);
        // include offeringId and enrolled count if possible
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
        return ResponseEntity.ok(Map.of("code", info.getCode(), "issuedAt", info.getIssuedAt(), "presentMinutes",
                info.getPresentWindowMinutes(), "lateMinutes", info.getLateWindowMinutes(), "offeringId", offeringId,
                "enrolledCount", enrolledCount));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam Long scheduleId) {
        attendanceCodeService.delete(scheduleId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/current")
    public ResponseEntity<?> current(@RequestParam Long scheduleId) {
        var info = attendanceCodeService.get(scheduleId);
        if (info == null)
            return ResponseEntity.ok(Map.of());
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
        return ResponseEntity.ok(Map.of("code", info.getCode(), "issuedAt", info.getIssuedAt(), "presentMinutes",
                info.getPresentWindowMinutes(), "lateMinutes", info.getLateWindowMinutes(), "offeringId", offeringId,
                "enrolledCount", enrolledCount));
    }

    // student enters a code
    @PostMapping("/enter")
    public ResponseEntity<?> enter(@RequestBody Map<String, Object> body) {
        try {
            Long scheduleId = body.get("scheduleId") == null ? null : ((Number) body.get("scheduleId")).longValue();
            String code = body.get("code") == null ? null : body.get("code").toString();
            if (scheduleId == null || code == null)
                return ResponseEntity.badRequest().body(Map.of("error", "scheduleId and code required"));

            var info = attendanceCodeService.get(scheduleId);
            if (info == null || !info.getCode().equals(code)) {
                return ResponseEntity.status(403).body(Map.of("error", "Invalid or expired code"));
            }

            Long currentUserId = securityHelper.getCurrentUserId();
            if (currentUserId == null)
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

            var scheduleOpt = classScheduleRepository.findById(scheduleId);
            if (scheduleOpt.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Schedule not found"));
            var schedule = scheduleOpt.get();

            Long offeringId = schedule.getOffering() == null ? null : schedule.getOffering().getId();
            if (offeringId == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Offering not found for schedule"));

            var enrollmentOpt = enrollmentRepository.findByStudentIdAndOfferingId(currentUserId, offeringId);
            if (enrollmentOpt.isEmpty())
                return ResponseEntity.status(403).body(Map.of("error", "Student not enrolled in offering"));
            var enrollment = enrollmentOpt.get();

            LocalDate date = LocalDate.now();
            boolean exists = attendanceRepository.existsByStudentIdAndScheduleId(currentUserId, scheduleId,
                    enrollment.getId(), date);
            if (exists) {
                return ResponseEntity.ok(Map.of("status", "exists"));
            }

            // compute status by time windows relative to schedule.startTime
            LocalTime start = schedule.getStartTime();
            java.time.LocalTime now = java.time.LocalTime.now(ZoneId.systemDefault());
            String status = "PRESENT";
            Integer presentWindow = null;
            Integer lateWindow = null;
            var codeInfo = attendanceCodeService.get(scheduleId);
            if (codeInfo != null) {
                presentWindow = codeInfo.getPresentWindowMinutes();
                lateWindow = codeInfo.getLateWindowMinutes();
            }
            if (presentWindow == null)
                presentWindow = 15; // default
            if (lateWindow == null)
                lateWindow = 30; // default
            if (start != null) {
                java.time.LocalTime presentCut = start.plusMinutes(presentWindow);
                java.time.LocalTime lateCut = start.plusMinutes(lateWindow);
                if (now.isAfter(presentCut) && !now.isAfter(lateCut)) {
                    status = "LATE";
                } else if (now.isAfter(lateCut)) {
                    status = "ABSENT";
                } else {
                    status = "PRESENT";
                }
            }

            Attendance a = new Attendance();
            a.setEnrollment(enrollment);
            a.setSchedule(schedule); // Use the already-fetched schedule entity
            a.setAttendanceDate(date);
            a.setStatus(status);
            User me = new User();
            me.setId(currentUserId);
            a.setRecordedBy(me);

            var saved = attendanceRepository.save(a);
            return ResponseEntity.ok(Map.of("saved", true, "attendanceId", saved.getId(), "status", status));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }
}
