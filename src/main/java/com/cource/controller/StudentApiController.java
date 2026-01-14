package com.cource.controller;

import com.cource.dto.enrollment.EnrollmentResult;
import com.cource.entity.CourseOffering;
import com.cource.service.EnrollmentService;
import com.cource.service.StudentService;
import com.cource.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
public class StudentApiController {

    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final SecurityHelper securityHelper;

    @GetMapping("/{studentId}/available-courses")
    public ResponseEntity<List<CourseOffering>> getAvailableCourses(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) String keyword) {
        var list = studentService.getAvailableOfferings(studentId, termId, keyword);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{studentId}/enroll/{offeringId}")
    public ResponseEntity<?> enrollInOffering(@PathVariable Long studentId, @PathVariable Long offeringId,
            @RequestParam(required = false) String enrollmentCode,
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            Long currentUserId = securityHelper.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("status", "UNAUTHORIZED", "message", "Not authenticated"));
            }
            if (!currentUserId.equals(studentId) && !securityHelper.hasRole("ROLE_ADMIN")) {
                return ResponseEntity.status(403).body(Map.of("status", "FORBIDDEN", "message", "Not allowed"));
            }

            if (enrollmentCode == null && payload != null) {
                enrollmentCode = payload.get("enrollmentCode");
            }
            if (enrollmentCode == null) {
                return ResponseEntity.badRequest().body("Enrollment code is required");
            }

            EnrollmentResult result = enrollmentService.enrollStudentWithOfferingCode(studentId, offeringId,
                    enrollmentCode);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ia) {
            return ResponseEntity.badRequest().body(ia.getMessage());
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(ise.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @DeleteMapping("/{studentId}/waitlist/{offeringId}")
    public ResponseEntity<?> removeFromWaitlist(@PathVariable Long studentId, @PathVariable Long offeringId) {
        Long currentUserId = securityHelper.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body(Map.of("status", "UNAUTHORIZED", "message", "Not authenticated"));
        }
        if (!currentUserId.equals(studentId) && !securityHelper.hasRole("ROLE_ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("status", "FORBIDDEN", "message", "Not allowed"));
        }
        EnrollmentResult result = enrollmentService.removeFromWaitlist(studentId, offeringId);
        return ResponseEntity.ok(result);
    }
}
