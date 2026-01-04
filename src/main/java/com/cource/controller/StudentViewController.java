package com.cource.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cource.service.StudentService;
import com.cource.util.SecurityHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentViewController {

    private final StudentService studentService;
    private final SecurityHelper securityHelper;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long studentId, Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            model.addAttribute("enrollments", studentService.getMyEnrollments(studentId));
            model.addAttribute("terms", studentService.getActiveTerms());
            model.addAttribute("gpa", studentService.calculateGPA(studentId));
            model.addAttribute("creditsEarned", studentService.getCreditsEarned(studentId));
            model.addAttribute("coursesCompleted", studentService.getCoursesCompleted(studentId));
        }
        return "views/student/dashboard";
    }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) Long studentId, Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            model.addAttribute("terms", studentService.getAllTerms());
        }
        return "views/student/courses";
    }

    @GetMapping("/my-courses")
    public String myCourses(@RequestParam(required = false) Long studentId, Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            model.addAttribute("enrollments", studentService.getMyEnrollments(studentId));
        }
        return "views/student/my-courses";
    }

    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) Long studentId, Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            var sched = studentService.getMySchedule(studentId);
            if (sched == null || sched.isEmpty()) {
                log.info("Student {} schedule is empty or null", studentId);
            } else {
                log.info("Student {} schedule entries: {}", studentId, sched.size());
                sched.forEach(s -> log.debug("Schedule entry: offeringId={}, day={}, start={}, end={}",
                        s.getOffering() != null ? s.getOffering().getId() : null,
                        s.getDayOfWeek(), s.getStartTime(), s.getEndTime()));
            }
            model.addAttribute("schedule", sched);
        }
        return "views/student/schedule";
    }

    @GetMapping("/grades")
    public String grades(@RequestParam(required = false) Long studentId, Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            model.addAttribute("grades", studentService.getMyGrades(studentId));
            // Add summary values so the grades page can display GPA, credits and completed
            // count
            model.addAttribute("gpa", studentService.calculateGPA(studentId));
            model.addAttribute("creditsEarned", studentService.getCreditsEarned(studentId));
            model.addAttribute("coursesCompleted", studentService.getCoursesCompleted(studentId));
        }
        return "views/student/grades";
    }

    @GetMapping("/attendance")
    public String attendance(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long offeringId,
            Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            // Only invoke StudentService methods when the current principal has the STUDENT
            // role.
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isStudent = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()));
            if (isStudent) {
                model.addAttribute("enrollments", studentService.getMyEnrollments(studentId));
                if (offeringId != null) {
                    model.addAttribute("offeringId", offeringId);
                    model.addAttribute("attendance", studentService.getMyAttendance(studentId, offeringId));
                    model.addAttribute("percentage", studentService.getAttendancePercentage(studentId, offeringId));
                    var schedules = studentService.getMySchedule(studentId).stream()
                            .filter(s -> s.getOffering() != null && s.getOffering().getId().equals(offeringId))
                            .toList();
                    model.addAttribute("schedules", schedules);
                }
            } else {
                // For non-student principals, avoid calling secured service methods to prevent
                // AuthorizationDeniedException during view rendering. Provide safe defaults.
                model.addAttribute("enrollments", java.util.Collections.emptyList());
                if (offeringId != null) {
                    model.addAttribute("offeringId", offeringId);
                    model.addAttribute("attendance", java.util.Collections.emptyList());
                    model.addAttribute("percentage", 0.0);
                    model.addAttribute("schedules", java.util.Collections.emptyList());
                }
            }
        }
        return "views/student/attendance";
    }

    @GetMapping("/enter-code")
    public String enterCode(@RequestParam(required = false) Long studentId, Model model) {
        if (studentId != null)
            model.addAttribute("studentId", studentId);
        return "views/student/enter-code";
    }

    @GetMapping("/waitlist")
    public String waitlist(@RequestParam(required = false) Long studentId, Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            model.addAttribute("waitlistEntries", studentService.getMyWaitlistEntries(studentId));
        }
        return "views/student/waitlist";
    }

    // Export endpoints
    @GetMapping("/grades/export")
    public ResponseEntity<String> exportGrades(@RequestParam Long studentId) {
        StringBuilder csv = new StringBuilder();
        csv.append("Course Code,Course Title,Credits,Grade,Status\n");

        for (var enrollment : studentService.getMyGrades(studentId)) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                    enrollment.getOffering().getCourse().getCourseCode(),
                    enrollment.getOffering().getCourse().getTitle(),
                    enrollment.getOffering().getCourse().getCredits(),
                    enrollment.getGrade() != null ? enrollment.getGrade() : "N/A",
                    enrollment.getStatus()));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my-grades.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    @GetMapping("/schedule/export")
    public ResponseEntity<String> exportSchedule(@RequestParam Long studentId) {
        StringBuilder csv = new StringBuilder();
        csv.append("Course,Day,Start Time,End Time,Room,Building\n");

        for (var schedule : studentService.getMySchedule(studentId)) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s\n",
                    schedule.getOffering().getCourse().getTitle(),
                    schedule.getDayOfWeek(),
                    schedule.getStartTime(),
                    schedule.getEndTime(),
                    schedule.getRoom().getRoomNumber(),
                    schedule.getRoom().getBuilding()));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my-schedule.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }
}
