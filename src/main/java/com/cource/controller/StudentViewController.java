package com.cource.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;

import com.cource.service.StudentService;
import com.cource.service.StudentReadService;
import com.cource.util.SecurityHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/student")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','LECTURER')")
public class StudentViewController {

    private final StudentService studentService;
    private final StudentReadService studentReadService;
    private final SecurityHelper securityHelper;
    private final com.cource.repository.CourseLecturerRepository courseLecturerRepository;
    private final com.cource.repository.EnrollmentRepository enrollmentRepository;

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long offeringId,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()));
        boolean isLecturer = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_LECTURER".equals(a.getAuthority()));

        Long currentUserId = securityHelper.getCurrentUserId();

        if (isStudent) {
            Long effectiveStudentId = studentId != null ? studentId : currentUserId;
            if (effectiveStudentId == null || currentUserId == null || !effectiveStudentId.equals(currentUserId)) {
                return "error/403";
            }

            model.addAttribute("studentId", effectiveStudentId);
            model.addAttribute("userId", effectiveStudentId);
            model.addAttribute("role", "STUDENT");
            model.addAttribute("enrollments", studentService.getMyEnrollments(effectiveStudentId));
            model.addAttribute("terms", studentService.getActiveTerms());
            model.addAttribute("gpa", studentService.calculateGPA(effectiveStudentId));
            model.addAttribute("creditsEarned", studentService.getCreditsEarned(effectiveStudentId));
            model.addAttribute("coursesCompleted", studentService.getCoursesCompleted(effectiveStudentId));
            return "student/dashboard";
        }

        if (isLecturer) {
            if (studentId == null || offeringId == null || currentUserId == null) {
                return "error/403";
            }

            boolean ownsOffering = courseLecturerRepository.existsByOfferingIdAndLecturerId(offeringId, currentUserId);
            boolean studentEnrolled = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId)
                    .isPresent();
            if (!ownsOffering || !studentEnrolled) {
                return "error/403";
            }

            model.addAttribute("studentId", studentId);
            model.addAttribute("userId", currentUserId);
            model.addAttribute("role", "LECTURER");

            var enrollments = studentReadService.getEnrollments(studentId).stream()
                    .filter(e -> e.getOffering() != null && e.getOffering().getId().equals(offeringId))
                    .toList();

            model.addAttribute("enrollments", enrollments);
            model.addAttribute("terms", studentReadService.getActiveTerms());
            model.addAttribute("gpa", studentReadService.calculateGPA(studentId));
            model.addAttribute("creditsEarned", studentReadService.getCreditsEarned(studentId));
            model.addAttribute("coursesCompleted", studentReadService.getCoursesCompleted(studentId));
            return "student/dashboard";
        }

        return "error/403";
    }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) Long studentId, Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            model.addAttribute("userId", studentId);
            model.addAttribute("role", "STUDENT");
            model.addAttribute("terms", studentService.getAllTerms());
        }
        return "student/courses";
    }

    @GetMapping("/my-courses")
    public String myCourses(@RequestParam(required = false) Long studentId, Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            model.addAttribute("userId", studentId);
            model.addAttribute("role", "STUDENT");
            model.addAttribute("enrollments", studentService.getMyEnrollments(studentId));
        }
        return "student/my-courses";
    }

    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) Long studentId, Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            model.addAttribute("userId", studentId);
            model.addAttribute("role", "STUDENT");
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
        return "student/schedule";
    }

    @GetMapping("/grades")
    public String grades(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long offeringId,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()));
        boolean isLecturer = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_LECTURER".equals(a.getAuthority()));

        Long currentUserId = securityHelper.getCurrentUserId();

        if (isStudent) {
            Long effectiveStudentId = studentId != null ? studentId : currentUserId;
            if (effectiveStudentId == null || currentUserId == null || !effectiveStudentId.equals(currentUserId)) {
                return "error/403";
            }

            model.addAttribute("studentId", effectiveStudentId);
            model.addAttribute("userId", effectiveStudentId);
            model.addAttribute("role", "STUDENT");
            model.addAttribute("grades", studentService.getMyGrades(effectiveStudentId));
            model.addAttribute("gpa", studentService.calculateGPA(effectiveStudentId));
            model.addAttribute("creditsEarned", studentService.getCreditsEarned(effectiveStudentId));
            model.addAttribute("coursesCompleted", studentService.getCoursesCompleted(effectiveStudentId));
            return "student/grades";
        }

        if (isLecturer) {
            if (studentId == null || offeringId == null || currentUserId == null) {
                return "error/403";
            }

            boolean ownsOffering = courseLecturerRepository.existsByOfferingIdAndLecturerId(offeringId, currentUserId);
            boolean studentEnrolled = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId)
                    .isPresent();
            if (!ownsOffering || !studentEnrolled) {
                return "error/403";
            }

            var grades = studentReadService.getGrades(studentId).stream()
                    .filter(e -> e.getOffering() != null && e.getOffering().getId() != null
                            && e.getOffering().getId().equals(offeringId))
                    .toList();

            // Compute summary for this offering only (avoid leaking other course grades).
            double totalPoints = 0.0;
            int totalCredits = 0;
            int creditsEarned = 0;
            int coursesCompleted = 0;
            for (var e : grades) {
                String g = e.getGrade();
                if (g == null) {
                    continue;
                }
                int credits;
                try {
                    credits = e.getOffering().getCourse().getCredits();
                } catch (Exception ex) {
                    credits = 0;
                }
                double pts = switch (g.trim().toUpperCase()) {
                    case "A+", "A" -> 4.0;
                    case "A-" -> 3.7;
                    case "B+" -> 3.3;
                    case "B" -> 3.0;
                    case "B-" -> 2.7;
                    case "C+" -> 2.3;
                    case "C" -> 2.0;
                    case "C-" -> 1.7;
                    case "D+" -> 1.3;
                    case "D" -> 1.0;
                    case "F", "W", "I" -> 0.0;
                    default -> 0.0;
                };

                totalPoints += pts * credits;
                totalCredits += credits;

                if (!(g.equalsIgnoreCase("F") || g.equalsIgnoreCase("W") || g.equalsIgnoreCase("I"))) {
                    creditsEarned += credits;
                    coursesCompleted++;
                }
            }
            double gpa = totalCredits == 0 ? 0.0 : Math.round((totalPoints / totalCredits) * 100.0) / 100.0;

            model.addAttribute("studentId", studentId);
            model.addAttribute("offeringId", offeringId);
            model.addAttribute("userId", currentUserId);
            model.addAttribute("role", "LECTURER");
            model.addAttribute("grades", grades);
            model.addAttribute("gpa", gpa);
            model.addAttribute("creditsEarned", creditsEarned);
            model.addAttribute("coursesCompleted", coursesCompleted);
            return "student/grades";
        }

        return "error/403";
    }

    @GetMapping("/attendance")
    public String attendance(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long offeringId,
            Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            model.addAttribute("userId", studentId);
            model.addAttribute("role", "STUDENT");
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
        return "student/attendance";
    }

    @GetMapping("/enter-code")
    public String enterCode(@RequestParam(required = false) Long studentId, Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            model.addAttribute("userId", studentId);
            model.addAttribute("role", "STUDENT");
        }
        return "student/enter-code";
    }

    @GetMapping("/waitlist")
    public String waitlist(@RequestParam(required = false) Long studentId, Model model) {
        if (studentId != null) {
            model.addAttribute("studentId", studentId);
            model.addAttribute("userId", studentId);
            model.addAttribute("role", "STUDENT");
            model.addAttribute("waitlistEntries", studentService.getMyWaitlistEntries(studentId));
        }
        return "student/waitlist";
    }

    // Export endpoints
    @GetMapping("/grades/export")
    public ResponseEntity<String> exportGrades(@RequestParam Long studentId,
            @RequestParam(required = false) Long offeringId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()));
        boolean isLecturer = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_LECTURER".equals(a.getAuthority()));

        Long currentUserId = securityHelper.getCurrentUserId();

        if (isStudent) {
            if (currentUserId == null || !currentUserId.equals(studentId)) {
                return ResponseEntity.status(403).build();
            }
        } else if (isLecturer) {
            if (offeringId == null || currentUserId == null) {
                return ResponseEntity.status(403).build();
            }
            boolean ownsOffering = courseLecturerRepository.existsByOfferingIdAndLecturerId(offeringId, currentUserId);
            boolean studentEnrolled = enrollmentRepository.findByStudentIdAndOfferingId(studentId, offeringId)
                    .isPresent();
            if (!ownsOffering || !studentEnrolled) {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.status(403).build();
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Course Code,Course Title,Credits,Grade,Status\n");

        var rows = isLecturer
                ? studentReadService.getGrades(studentId).stream()
                        .filter(e -> offeringId == null
                                || (e.getOffering() != null && offeringId.equals(e.getOffering().getId())))
                        .toList()
                : studentService.getMyGrades(studentId);

        for (var enrollment : rows) {
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
