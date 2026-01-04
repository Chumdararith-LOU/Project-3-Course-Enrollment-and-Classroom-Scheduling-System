package com.cource.controller;

import com.cource.dto.course.CourseResponseDTO;
import com.cource.dto.lecturer.LecturerDashboardDTO;
import com.cource.entity.*;
import com.cource.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cource.exception.ResourceNotFoundException;
import com.cource.service.LecturerService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/lecturer")
@RequiredArgsConstructor
public class LecturerViewController {

    private final LecturerService lecturerService;
    private final SecurityHelper securityHelper;

    @GetMapping("/dashboard")
    public String getDashboard(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) {
            lecturerId = securityHelper.getCurrentUserId();
        }

        LecturerDashboardDTO stats = lecturerService.getDashboardStats(lecturerId);

        model.addAttribute("dashboard", stats);
        model.addAttribute("lecturerId", lecturerId);

        return "lecturer/dashboard";
    }

    @GetMapping("/schedule")
    public String getSchedulePage(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) lecturerId = securityHelper.getCurrentUserId();

        model.addAttribute("lecturerId", lecturerId);
        return "lecturer/schedule";
    }

    @GetMapping("/attendance")
    public String getAttendancePage(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) lecturerId = securityHelper.getCurrentUserId();

        model.addAttribute("lecturerId", lecturerId);
        return "lecturer/attendance";
    }

    @GetMapping("/reports")
    public String getReportsPage(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) lecturerId = securityHelper.getCurrentUserId();

        model.addAttribute("lecturerId", lecturerId);
        return "lecturer/reports";
    }

    @GetMapping("/courses")
    public String getCoursesPage(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) lecturerId = securityHelper.getCurrentUserId();

        List<CourseResponseDTO> courses = lecturerService.getCoursesByLecturerId(lecturerId);
        model.addAttribute("courses", courses);
        model.addAttribute("lecturerId", lecturerId);
        return "lecturer/courses";
    }

    @GetMapping("/courses/{offeringId}/schedule")
    public List<ClassSchedule> getClassSchedules(
            @PathVariable long offeringId,
            @RequestParam long lecturerId) {
        return lecturerService.getClassSchedulesByLecturerId(offeringId, lecturerId);
    }

    @GetMapping("/courses/{offeringId}/students")
    public List<Student> getEnrolledStudents(
            @PathVariable long offeringId,
            @RequestParam long lecturerId) {
        return lecturerService.getEnrolledStudents(offeringId, lecturerId);
    }

    @PostMapping("/attendance")
    public ResponseEntity<String> recordAttendance(
            @RequestBody com.cource.dto.attendance.AttendanceRequestDTO attendanceRequestDTO,
            @RequestParam long studentId,
            @RequestParam String status) {
        lecturerService.recordAttendance(attendanceRequestDTO, studentId, status);
        return ResponseEntity.ok("Attendance recorded successfully.");
    }

    @PutMapping("/attendance/{id}")
    public ResponseEntity<?> updateAttendance(@PathVariable("id") long attendanceId,
            @RequestBody com.cource.dto.attendance.AttendanceRequestDTO attendanceRequestDTO,
            @RequestParam(required = false) Long lecturerId) {
        try {
            var updated = lecturerService.updateAttendance(attendanceId, attendanceRequestDTO, lecturerId);
            // map to a simple DTO to avoid lazy serialization
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", updated.getId());
            m.put("attendanceDate", updated.getAttendanceDate());
            m.put("status", updated.getStatus());
            m.put("notes", updated.getNotes());
            if (updated.getEnrollment() != null && updated.getEnrollment().getStudent() != null) {
                var s = updated.getEnrollment().getStudent();
                java.util.Map<String, Object> sd = new java.util.LinkedHashMap<>();
                sd.put("id", s.getId());
                sd.put("fullName", s.getFullName());
                m.put("student", sd);
            }
            if (updated.getRecordedBy() != null) {
                var rb = updated.getRecordedBy();
                java.util.Map<String, Object> rbd = new java.util.LinkedHashMap<>();
                rbd.put("id", rb.getId());
                rbd.put("fullName", rb.getFullName());
                m.put("recordedBy", rbd);
            }
            return ResponseEntity.ok(m);
        } catch (ResourceNotFoundException rnfe) {
            return ResponseEntity.status(404).body(java.util.Collections.singletonMap("message", rnfe.getMessage()));
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(java.util.Collections.singletonMap("message", se.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Collections.singletonMap("message", ex.toString()));
        }
    }

    @DeleteMapping("/attendance/{id}")
    public ResponseEntity<?> deleteAttendance(@PathVariable("id") long attendanceId,
            @RequestParam(required = false) Long lecturerId) {
        try {
            lecturerService.deleteAttendance(attendanceId, lecturerId);
            return ResponseEntity.ok(java.util.Collections.singletonMap("status", "deleted"));
        } catch (ResourceNotFoundException rnfe) {
            return ResponseEntity.status(404).body(java.util.Collections.singletonMap("message", rnfe.getMessage()));
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(java.util.Collections.singletonMap("message", se.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Collections.singletonMap("message", ex.toString()));
        }
    }

    // --- Course offering CRUD for lecturers ---
    @PostMapping("/offerings")
    public ResponseEntity<?> createOffering(
            @RequestBody com.cource.dto.course.CourseOfferingRequestDTO dto,
            @RequestParam long lecturerId) {
        var offering = lecturerService.createCourseOffering(lecturerId, dto);
        return ResponseEntity.ok(offering);
    }

    @PutMapping("/offerings/{id}")
    public ResponseEntity<?> updateOffering(
            @PathVariable("id") long id,
            @RequestBody com.cource.dto.course.CourseOfferingRequestDTO dto,
            @RequestParam long lecturerId) {
        var offering = lecturerService.updateCourseOffering(lecturerId, id, dto);
        return ResponseEntity.ok(offering);
    }

    @GetMapping("/offerings/{id}")
    public ResponseEntity<?> getOffering(@PathVariable("id") long id, @RequestParam long lecturerId) {
        try {
            var offering = lecturerService.getOfferingById(lecturerId, id);
            return ResponseEntity.ok(offering);
        } catch (ResourceNotFoundException rnfe) {
            return ResponseEntity.status(404).body(java.util.Collections.singletonMap("message", rnfe.getMessage()));
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(java.util.Collections.singletonMap("message", se.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Collections.singletonMap("message", ex.toString()));
        }
    }

    @PostMapping("/offerings/{id}/regenerate")
    public ResponseEntity<?> regenerateOfferingCode(@PathVariable("id") long id, @RequestParam long lecturerId) {
        try {
            var offering = lecturerService.regenerateOfferingEnrollmentCode(lecturerId, id);
            return ResponseEntity
                    .ok(java.util.Collections.singletonMap("enrollmentCode", offering.getEnrollmentCode()));
        } catch (ResourceNotFoundException rnfe) {
            return ResponseEntity.status(404).body(java.util.Collections.singletonMap("message", rnfe.getMessage()));
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(java.util.Collections.singletonMap("message", se.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Collections.singletonMap("message", ex.toString()));
        }
    }

    @DeleteMapping("/offerings/{id}")
    public ResponseEntity<?> deleteOffering(@PathVariable("id") long id, @RequestParam long lecturerId) {
        lecturerService.deleteCourseOffering(lecturerId, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/attendance/{scheduleId}")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getAttendanceRecords(
            @PathVariable long scheduleId,
            @RequestParam(required = false) Long lecturerId) {
        try {
            var list = lecturerService.getAttendanceRecordsAsDto(scheduleId, lecturerId);
            return ResponseEntity.ok(list);
        } catch (ResourceNotFoundException rnfe) {
            return ResponseEntity.status(404).body(java.util.Collections.emptyList());
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Collections.emptyList());
        }
    }

    @GetMapping("/attendance/trends")
    public ResponseEntity<java.util.Map<String, Long>> getAttendanceTrends(
            @RequestParam long lecturerId,
            @RequestParam(required = false, defaultValue = "14") int days) {
        var map = lecturerService.getAttendanceCountsByDate(lecturerId, days);
        return ResponseEntity.ok(map);
    }

    @GetMapping("/courses/averages")
    public ResponseEntity<java.util.Map<String, Double>> getCourseAverages(@RequestParam long lecturerId) {
        var map = lecturerService.getCourseAverageGradeByLecturer(lecturerId);
        return ResponseEntity.ok(map);
    }

}
