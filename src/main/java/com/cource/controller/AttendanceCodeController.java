package com.cource.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.cource.service.AttendanceCodeService;
import com.cource.service.AttendanceCodeApplicationService;
import com.cource.util.SecurityHelper;
import com.cource.dto.attendance.AttendanceCodeDetailsDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attendance-code")
@RequiredArgsConstructor
public class AttendanceCodeController {

    private final AttendanceCodeService attendanceCodeService;
    private final AttendanceCodeApplicationService attendanceCodeApplicationService;
    private final SecurityHelper securityHelper;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ResponseEntity<?> generate(@RequestParam Long scheduleId,
            @RequestParam(required = false) Integer presentMinutes,
            @RequestParam(required = false) Integer lateMinutes) {
        Long lecturerId = securityHelper.getCurrentUserId();
        AttendanceCodeDetailsDTO details = attendanceCodeApplicationService.generateDetails(scheduleId, lecturerId,
                presentMinutes, lateMinutes);
        return ResponseEntity.ok(Map.of("code", details.getCode(), "issuedAt", details.getIssuedAt(), "presentMinutes",
                details.getPresentMinutes(), "lateMinutes", details.getLateMinutes(), "offeringId",
                details.getOfferingId(), "enrolledCount", details.getEnrolledCount()));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ResponseEntity<?> delete(@RequestParam Long scheduleId) {
        attendanceCodeService.delete(scheduleId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ResponseEntity<?> current(@RequestParam Long scheduleId) {
        AttendanceCodeDetailsDTO details = attendanceCodeApplicationService.currentDetails(scheduleId);
        if (details == null)
            return ResponseEntity.ok(Map.of());
        return ResponseEntity.ok(Map.of("code", details.getCode(), "issuedAt", details.getIssuedAt(), "presentMinutes",
                details.getPresentMinutes(), "lateMinutes", details.getLateMinutes(), "offeringId",
                details.getOfferingId(), "enrolledCount", details.getEnrolledCount()));
    }

    @PostMapping("/enter")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<?> enter(@RequestBody Map<String, Object> body) {
        try {
            Long scheduleId = body.get("scheduleId") == null ? null : ((Number) body.get("scheduleId")).longValue();
            String code = body.get("code") == null ? null : body.get("code").toString();
            Long currentUserId = securityHelper.getCurrentUserId();
            if (currentUserId == null)
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            var result = attendanceCodeApplicationService.enterCode(scheduleId, code, currentUserId);
            if (result.isExists()) {
                return ResponseEntity.ok(Map.of("status", "exists"));
            }
            return ResponseEntity.ok(
                    Map.of("saved", true, "attendanceId", result.getAttendanceId(), "status", result.getStatus()));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }
}