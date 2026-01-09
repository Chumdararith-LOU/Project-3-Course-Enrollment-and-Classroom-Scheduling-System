package com.cource.controller;

import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentApiController {

    private final StudentService studentService;

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
            if (enrollmentCode == null && payload != null) {
                enrollmentCode = payload.get("enrollmentCode");
            }
            if (enrollmentCode == null) {
                return ResponseEntity.badRequest().body("Enrollment code is required");
            }
            Enrollment e = studentService.enrollInOffering(studentId, offeringId, enrollmentCode);
            return ResponseEntity.ok(e);
        } catch (IllegalArgumentException ia) {
            return ResponseEntity.badRequest().body(ia.getMessage());
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(ise.getMessage());
        }
    }
}
