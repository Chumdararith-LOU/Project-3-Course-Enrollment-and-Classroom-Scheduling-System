package com.cource.controller.Lecturer;

import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.CourseOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cource.service.LecturerService;
import com.cource.service.AdminService;
import com.cource.repository.RoleRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.EnrollmentRepository;

@Controller
@RequestMapping("/lecturer")
@PreAuthorize("hasRole('LECTURER')")
@RequiredArgsConstructor
public class LecturerViewController {
    private static final Logger log = LoggerFactory.getLogger(LecturerViewController.class);

    private final LecturerService lecturerService;
    private final AdminService adminService;
    private final RoleRepository roleRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;



    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("userId", lecturerId);
            model.addAttribute("role", "LECTURER");
        }

        if (lecturerId != null) {
            var offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
            int totalCourses = offerings == null ? 0 : offerings.size();
            long totalStudents = 0;
            int classesToday = 0;
            var today = java.time.LocalDate.now();
            if (offerings != null) {
                for (var off : offerings) {
                    var enrollments = enrollmentRepository.findByOfferingId(off.getId());
                    if (enrollments != null) {
                        totalStudents += enrollments.size();
                    }

                    var schedules = classScheduleRepository.findByOfferingId(off.getId());
                    if (schedules != null) {
                        String todayDow = today.getDayOfWeek().name().substring(0, 3).toUpperCase();
                        for (var s : schedules) {
                            if (s.getDayOfWeek() != null && s.getDayOfWeek().equals(todayDow)) {
                                classesToday++;
                            }
                        }
                    }
                }
            }
            model.addAttribute("totalCourses", totalCourses);
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("classesToday", classesToday);

            var attendanceMap = lecturerService.getAttendanceCountsByDate(lecturerId, 7);
            var enrollmentLabels = new ArrayList<>(attendanceMap.keySet());
            var enrollmentData = new ArrayList<Number>();
            for (String k : enrollmentLabels) {
                enrollmentData.add(attendanceMap.getOrDefault(k, 0L));
            }
            model.addAttribute("enrollmentLabels", enrollmentLabels);
            model.addAttribute("enrollmentData", enrollmentData);
        }

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            model.addAttribute("systemTotalStudents", adminService.getTotalStudents());
            model.addAttribute("systemTotalLecturers", adminService.getTotalLecturers());
        } else {
            model.addAttribute("systemTotalStudents", 0);
            model.addAttribute("systemTotalLecturers", 0);
        }

        var roles = roleRepository.findAll();
        var userLabels = new ArrayList<String>();
        var userData = new ArrayList<Number>();
        if (isAdmin) {
            for (var r : roles) {
                long count = adminService.getUsersByRole(r.getRoleCode()).size();
                userLabels.add(r.getRoleName());
                userData.add(count);
            }
        } else {
            for (var r : roles) {
                userLabels.add(r.getRoleName());
                userData.add(0);
            }
        }
        model.addAttribute("userLabels", userLabels);
        model.addAttribute("userData", userData);

        return "lecturer/dashboard";
    }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) Long lecturerId, Model model) {
        try {
            if (lecturerId != null) {
                model.addAttribute("lecturerId", lecturerId);
                model.addAttribute("userId", lecturerId); // ADD THIS LINE
                model.addAttribute("role", "LECTURER");
                model.addAttribute("offerings", lecturerService.getOfferingsByLecturerId(lecturerId));
            }
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = false;
            if (auth != null && auth.getAuthorities() != null) {
                isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            }
            if (isAdmin) {
                // Filter only active courses for offering creation dropdown
                var allCourses = adminService.getAllCourses();
                var activeCourses = allCourses.stream().filter(c -> c.isActive()).toList();
                model.addAttribute("courses", activeCourses);
                model.addAttribute("terms", adminService.getAllTerms());
            } else {
                model.addAttribute("courses", new ArrayList<>());
                model.addAttribute("terms", new ArrayList<>());
            }
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
        }
        return "lecturer/courses";
    }

    @GetMapping("/students")
    public String students(
            @RequestParam(required = false) Long offeringId,
            @RequestParam(required = false) Long lecturerId,
            Model model) {
        if (offeringId != null && lecturerId != null) {
            try {
                model.addAttribute("offeringId", offeringId);
                model.addAttribute("lecturerId", lecturerId);
                model.addAttribute("userId", lecturerId);
                model.addAttribute("role", "LECTURER");

                CourseOffering offering = courseOfferingRepository.findById(offeringId)
                        .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));
                if (offering.getLecturer() == null || !offering.getLecturer().getId().equals(lecturerId)) {
                    return "error/403";
                }

                var enrollments = enrollmentRepository.findByOfferingIdWithStudentFiltered(offeringId, "ENROLLED");
                model.addAttribute("enrollments", enrollments);

                List<com.cource.entity.User> students = new ArrayList<>();
                Map<Long, com.cource.entity.Enrollment> enrollmentMap = new HashMap<>();
                if (enrollments != null) {
                    for (com.cource.entity.Enrollment e : enrollments) {
                        if (e != null && e.getStudent() != null && e.getStudent().getId() != null) {
                            students.add(e.getStudent());
                            enrollmentMap.put(e.getStudent().getId(), e);
                        }
                    }
                }

                log.debug("LecturerViewController.students offeringId={}, enrollmentsCount={}, studentsCount={}",
                        offeringId, (enrollments == null ? 0 : enrollments.size()),
                        (students == null ? 0 : students.size()));

                model.addAttribute("students", students);
                model.addAttribute("enrollmentMap", enrollmentMap);
            } catch (Exception ex) {
                model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
            }
        }
        return "lecturer/students";
    }

    @PostMapping("/enrollments/{enrollmentId}/grade")
    public String updateEnrollmentGrade(
            @PathVariable Long enrollmentId,
            @RequestParam Long offeringId,
            @RequestParam Long lecturerId,
            @RequestParam(required = false) String grade) {
        lecturerService.updateEnrollmentGrade(lecturerId, enrollmentId, grade);
        return "redirect:/lecturer/students?offeringId=" + offeringId + "&lecturerId=" + lecturerId;
    }

    @GetMapping("/attendance")
    public String attendance(
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) Long lecturerId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long offeringId,
            Model model) {

        if (lecturerId != null && scheduleId == null) {
            try {
                var offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
                if (offerings != null) {
                    for (var off : offerings) {
                        var classSchedules = classScheduleRepository.findByOfferingId(off.getId());
                        if (classSchedules == null) {
                            continue;
                        }
                        if (classSchedules != null && !classSchedules.isEmpty()) {
                            scheduleId = classSchedules.get(0).getId();
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        // Provide schedules for the lecturer so the template can show a selector
        if (lecturerId != null) {
            try {
                List<com.cource.entity.ClassSchedule> schedules = new ArrayList<>();
                var offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
                if (offerings != null) {
                    for (var off : offerings) {
                        try {
                            var classSchedules = classScheduleRepository.findByOfferingId(off.getId());
                            if (classSchedules != null) {
                                schedules.addAll(classSchedules);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                model.addAttribute("schedules", schedules);
            } catch (Exception ignored) {
                model.addAttribute("schedules", List.of());
            }
        }

        if (studentId != null) {
            model.addAttribute("initialStudentId", studentId);
        }

        if (offeringId != null) {
            model.addAttribute("offeringId", offeringId);
        }

        if (scheduleId != null && lecturerId != null) {
            model.addAttribute("scheduleId", scheduleId);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("userId", lecturerId); // ADD THIS LINE
            model.addAttribute("role", "LECTURER");
            model.addAttribute("attendanceRecords", lecturerService.getAttendanceRecords(scheduleId, lecturerId));

            try {
                var schedOpt = classScheduleRepository.findById(scheduleId);
                if (schedOpt.isPresent() && schedOpt.get().getOffering() != null) {
                    Long offId = schedOpt.get().getOffering().getId();
                    var enrollments = enrollmentRepository.findByOfferingId(offId);
                    List<User> students = enrollments.stream()
                            .map(Enrollment::getStudent)
                            .filter(Objects::nonNull)
                            .toList();
                    model.addAttribute("students", students);
                } else {
                    model.addAttribute("students", List.of());
                }
            } catch (Exception ex) {
                model.addAttribute("students", List.of());
            }
        }
        return "lecturer/attendance";
    }

    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("userId", lecturerId);
            model.addAttribute("role", "LECTURER");

            var offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
            log.debug("Offerings for lecturerId={}, count={}", lecturerId, offerings.size());
            List<com.cource.entity.ClassSchedule> schedules = new ArrayList<>();
            for (var offering : offerings) {
                var classSchedules = classScheduleRepository.findByOfferingId(offering.getId());
                log.debug("Schedules for OfferingId={}: {}", offering.getId(),
                        (classSchedules != null ? classSchedules.size() : 0));
                if (classSchedules != null) {
                    schedules.addAll(classSchedules);
                }
            }
            log.debug("Total schedules found: {}", schedules.size());
            model.addAttribute("schedules", schedules);
            model.addAttribute("offerings", offerings); // <-- Add offerings for course selection
        } else {
            model.addAttribute("offerings", new ArrayList<>());
        }
        // Always provide all terms for the modal
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            model.addAttribute("terms", adminService.getAllTerms());
        } else {
            model.addAttribute("terms", List.of());
        }
        return "lecturer/schedule";
    }

    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false) Long lecturerId,
            @RequestParam(required = false) Long offeringId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String studentStatus,
            Model model) {
        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("userId", lecturerId);
            model.addAttribute("role", "LECTURER");
        }

        LocalDate fromDate = (from == null || from.isBlank()) ? LocalDate.now().minusDays(29) : LocalDate.parse(from);
        LocalDate toDate = (to == null || to.isBlank()) ? LocalDate.now() : LocalDate.parse(to);
        if (toDate.isBefore(fromDate)) {
            var tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        model.addAttribute("filterOfferingId", offeringId);
        model.addAttribute("filterFrom", fromDate.toString());
        model.addAttribute("filterTo", toDate.toString());
        model.addAttribute("filterStudentStatus", studentStatus);

        if (lecturerId != null) {
            // dropdown options
            var offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
            model.addAttribute("offerings", offerings);

            double avgAttendance = lecturerService.calculateAverageAttendance(lecturerId, fromDate, toDate, offeringId,
                    studentStatus);
            model.addAttribute("summaryAvgAttendance", avgAttendance);

            double passRate;
            if (offeringId != null) {
                passRate = lecturerService.calculatePassRate(lecturerId, offeringId, studentStatus);
            } else {
                double sum = 0.0;
                int n = 0;
                for (var off : offerings) {
                    sum += lecturerService.calculatePassRate(lecturerId, off.getId(), studentStatus);
                    n++;
                }
                passRate = n == 0 ? 0.0 : (sum / n);
            }
            model.addAttribute("summaryPassRate", passRate);

            List<Long> offeringIds = offerings == null ? List.of()
                    : offerings.stream().map(o -> o.getId()).toList();
            if (offeringId != null) {
                offeringIds = List.of(offeringId);
            }

            String statusToCount = (studentStatus == null || studentStatus.isBlank()) ? "ENROLLED" : studentStatus;
            long activeEnrollments = offeringIds.isEmpty() ? 0
                    : enrollmentRepository.countByOfferingIdsAndStatus(offeringIds, statusToCount);
            model.addAttribute("summaryActiveEnrollments", activeEnrollments);

            long totalClasses = offeringIds.isEmpty() ? 0 : classScheduleRepository.countByOfferingIds(offeringIds);
            model.addAttribute("summaryTotalClasses", totalClasses);

            model.addAttribute("courseReports", lecturerService.getCourseReports(lecturerId, fromDate, toDate,
                    studentStatus));

            if (offeringId != null) {
                var detail = lecturerService.getDetailedCourseReport(lecturerId, offeringId, fromDate, toDate,
                        studentStatus);
                model.addAttribute("courseDetail", detail);

                var gradeLabels = new ArrayList<String>();
                var gradeData = new ArrayList<Number>();
                if (detail != null && detail.getGradeDistribution() != null) {
                    for (var e : detail.getGradeDistribution().entrySet()) {
                        gradeLabels.add(e.getKey());
                        gradeData.add(e.getValue());
                    }
                }
                model.addAttribute("gradeLabels", gradeLabels);
                model.addAttribute("gradeData", gradeData);
            } else {
                model.addAttribute("courseDetail", null);
                model.addAttribute("gradeLabels", List.of());
                model.addAttribute("gradeData", List.of());
            }
        } else {
            model.addAttribute("offerings", List.of());
            model.addAttribute("summaryAvgAttendance", 0.0);
            model.addAttribute("summaryPassRate", 0.0);
            model.addAttribute("summaryActiveEnrollments", 0L);
            model.addAttribute("summaryTotalClasses", 0L);
            model.addAttribute("courseReports", List.of());
            model.addAttribute("courseDetail", null);
            model.addAttribute("gradeLabels", List.of());
            model.addAttribute("gradeData", List.of());
        }

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            model.addAttribute("totalStudents", adminService.getTotalStudents());
            model.addAttribute("totalLecturers", adminService.getTotalLecturers());
        } else {
            model.addAttribute("totalStudents", 0);
            model.addAttribute("totalLecturers", 0);
        }

        if (lecturerId != null) {
            var attendanceMap = lecturerService.getAttendanceCountsByDateRange(lecturerId, fromDate, toDate, offeringId,
                    studentStatus);
            var attendanceLabels = new ArrayList<String>(attendanceMap.keySet());
            var attendanceData = new ArrayList<Number>();
            for (String k : attendanceLabels) {
                attendanceData.add(attendanceMap.getOrDefault(k, 0L));
            }
            model.addAttribute("enrollmentLabels", attendanceLabels);
            model.addAttribute("enrollmentData", attendanceData);

            var perf = lecturerService.getCourseAverageGradeByLecturer(lecturerId);
            var courseLabels = new ArrayList<String>(perf.keySet());
            var courseData = new ArrayList<Number>();
            for (String k : courseLabels) {
                courseData.add(perf.getOrDefault(k, 0.0));
            }
            model.addAttribute("courseLabels", courseLabels);
            model.addAttribute("courseData", courseData);
        } else {
            model.addAttribute("enrollmentLabels", List.of());
            model.addAttribute("enrollmentData", List.of());
            model.addAttribute("courseLabels", List.of());
            model.addAttribute("courseData", List.of());
        }
        return "lecturer/reports";
    }
}
