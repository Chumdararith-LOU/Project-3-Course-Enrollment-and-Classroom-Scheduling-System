package com.cource.controller.Lecturer;

import com.cource.entity.ClassSchedule;
import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.*;
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
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long lecturerId, Model model) {
        lecturerId = resolveLecturerId(lecturerId);
        populateCommonModel(model, lecturerId);


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

            // Calculate attendance rate
            double attendanceRate = 0.0;
            if (lecturerId != null && totalCourses > 0) {
                attendanceRate = lecturerService.calculateAverageAttendance(lecturerId,
                        LocalDate.now().minusDays(30),
                        LocalDate.now(),
                        null,
                        "ENROLLED");
            }
            model.addAttribute("attendanceRate", attendanceRate);

            var attendanceMap = lecturerService.getAttendanceCountsByDate(lecturerId, 7);
            var enrollmentLabels = new ArrayList<String>();
            var enrollmentData = new ArrayList<Number>();
            if (attendanceMap != null && !attendanceMap.isEmpty()) {
                enrollmentLabels = new ArrayList<>(attendanceMap.keySet());
                for (String k : enrollmentLabels) {
                    enrollmentData.add(attendanceMap.getOrDefault(k, 0L));
                }
            }
            model.addAttribute("enrollmentLabels", enrollmentLabels);
            model.addAttribute("enrollmentData", enrollmentData);
        } else {
            // Provide default values when lecturerId is null
            model.addAttribute("totalCourses", 0);
            model.addAttribute("totalStudents", 0);
            model.addAttribute("classesToday", 0);
            model.addAttribute("attendanceRate", 0.0);
            model.addAttribute("enrollmentLabels", new ArrayList<String>());
            model.addAttribute("enrollmentData", new ArrayList<Number>());
        }

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            model.addAttribute("systemTotalStudents", adminService.getTotalStudents());
            model.addAttribute("systemTotalLecturers", adminService.getTotalLecturers());

            var roles = roleRepository.findAll();
            var userLabels = new ArrayList<String>();
            var userData = new ArrayList<Number>();
            for (var r : roles) {
                long count = adminService.getUsersByRole(r.getRoleCode()).size();
                userLabels.add(r.getRoleName());
                userData.add(count);
            }
            model.addAttribute("userLabels", userLabels);
            model.addAttribute("userData", userData);
        } else {
            model.addAttribute("systemTotalStudents", 0);
            model.addAttribute("systemTotalLecturers", 0);
            model.addAttribute("userLabels", new ArrayList<>());
            model.addAttribute("userData", new ArrayList<>());
        }

        return "lecturer/dashboard";
    }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) Long lecturerId, Model model) {
        lecturerId = resolveLecturerId(lecturerId);
        populateCommonModel(model, lecturerId);

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

        lecturerId = resolveLecturerId(lecturerId);
        populateCommonModel(model, lecturerId);

        if (offeringId != null && lecturerId != null) {
            try {
                model.addAttribute("offeringId", offeringId);

                CourseOffering offering = courseOfferingRepository.findById(offeringId)
                        .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));

                // Security check
                if (offering.getLecturer() == null || !offering.getLecturer().getId().equals(lecturerId)) {
                    return "error/403";
                }

                var enrollments = enrollmentRepository.findByOfferingIdWithStudentFiltered(offeringId, "ENROLLED");
                model.addAttribute("enrollments", enrollments);

                List<User> students = new ArrayList<>();
                Map<Long, Enrollment> enrollmentMap = new HashMap<>();
                if (enrollments != null) {
                    for (Enrollment e : enrollments) {
                        if (e != null && e.getStudent() != null && e.getStudent().getId() != null) {
                            students.add(e.getStudent());
                            enrollmentMap.put(e.getStudent().getId(), e);
                        }
                    }
                }
                model.addAttribute("students", students);
                model.addAttribute("enrollmentMap", enrollmentMap);
            } catch (Exception ex) {
                model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
                model.addAttribute("students", new ArrayList<>());
                model.addAttribute("enrollmentMap", new HashMap<>());
            }
        } else {
            model.addAttribute("students", new ArrayList<>());
            model.addAttribute("enrollmentMap", new HashMap<>());
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

        lecturerId = resolveLecturerId(lecturerId);
        populateCommonModel(model, lecturerId);

        // If no schedule selected, try to find a default one from the lecturer's courses
        if (lecturerId != null && scheduleId == null) {
            try {
                var offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
                if (offerings != null) {
                    for (var off : offerings) {
                        var classSchedules = classScheduleRepository.findByOfferingId(off.getId());
                        if (classSchedules != null && !classSchedules.isEmpty()) {
                            scheduleId = classSchedules.get(0).getId();
                            break;
                        }
                    }
                }
            } catch (Exception ignored) { }
        }

        // Provide schedules dropdown
        if (lecturerId != null) {
            try {
                List<ClassSchedule> schedules = new ArrayList<>();
                var offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
                if (offerings != null) {
                    for (var off : offerings) {
                        try {
                            var classSchedules = classScheduleRepository.findByOfferingId(off.getId());
                            if (classSchedules != null) {
                                schedules.addAll(classSchedules);
                            }
                        } catch (Exception ignored) { }
                    }
                }
                model.addAttribute("schedules", schedules);
            } catch (Exception ignored) {
                model.addAttribute("schedules", List.of());
            }
        } else {
            model.addAttribute("schedules", List.of());
        }

        if (studentId != null) model.addAttribute("initialStudentId", studentId);
        if (offeringId != null) model.addAttribute("offeringId", offeringId);

        if (scheduleId != null && lecturerId != null) {
            model.addAttribute("scheduleId", scheduleId);
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
        lecturerId = resolveLecturerId(lecturerId);
        populateCommonModel(model, lecturerId);

        if (lecturerId != null) {
            var offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
            List<ClassSchedule> schedules = new ArrayList<>();
            for (var offering : offerings) {
                var classSchedules = classScheduleRepository.findByOfferingId(offering.getId());
                if (classSchedules != null) {
                    schedules.addAll(classSchedules);
                }
            }
            model.addAttribute("schedules", schedules);
            model.addAttribute("offerings", offerings);
        } else {
            model.addAttribute("offerings", new ArrayList<>());
            model.addAttribute("schedules", new ArrayList<>());
        }

        // Always provide all terms for the modal (Admin checks inside template or restricted logic)
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

        lecturerId = resolveLecturerId(lecturerId);
        populateCommonModel(model, lecturerId);

        LocalDate fromDate = (from == null || from.isBlank()) ? LocalDate.now().minusDays(29) : LocalDate.parse(from);
        LocalDate toDate = (to == null || to.isBlank()) ? LocalDate.now() : LocalDate.parse(to);
        if (toDate.isBefore(fromDate)) {
            var tmp = fromDate; fromDate = toDate; toDate = tmp;
        }

        model.addAttribute("filterOfferingId", offeringId);
        model.addAttribute("filterFrom", fromDate.toString());
        model.addAttribute("filterTo", toDate.toString());
        model.addAttribute("filterStudentStatus", studentStatus);

        if (lecturerId != null) {
            var offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
            model.addAttribute("offerings", offerings);

            double avgAttendance = lecturerService.calculateAverageAttendance(lecturerId, fromDate, toDate, offeringId, studentStatus);
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

            List<Long> offeringIds = offerings == null ? List.of() : offerings.stream().map(CourseOffering::getId).toList();
            if (offeringId != null) {
                offeringIds = List.of(offeringId);
            }

            String statusToCount = (studentStatus == null || studentStatus.isBlank()) ? "ENROLLED" : studentStatus;
            long activeEnrollments = offeringIds.isEmpty() ? 0 : enrollmentRepository.countByOfferingIdsAndStatus(offeringIds, statusToCount);
            model.addAttribute("summaryActiveEnrollments", activeEnrollments);

            long totalClasses = offeringIds.isEmpty() ? 0 : classScheduleRepository.countByOfferingIds(offeringIds);
            model.addAttribute("summaryTotalClasses", totalClasses);

            model.addAttribute("courseReports", lecturerService.getCourseReports(lecturerId, fromDate, toDate, studentStatus));

            if (offeringId != null) {
                var detail = lecturerService.getDetailedCourseReport(lecturerId, offeringId, fromDate, toDate, studentStatus);
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

            // Charts
            var attendanceMap = lecturerService.getAttendanceCountsByDateRange(lecturerId, fromDate, toDate, offeringId, studentStatus);
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
            // Default empty model for reports if no lecturer found
            model.addAttribute("offerings", List.of());
            model.addAttribute("summaryAvgAttendance", 0.0);
            model.addAttribute("summaryPassRate", 0.0);
            model.addAttribute("summaryActiveEnrollments", 0L);
            model.addAttribute("summaryTotalClasses", 0L);
            model.addAttribute("courseReports", List.of());
            model.addAttribute("courseDetail", null);
            model.addAttribute("gradeLabels", List.of());
            model.addAttribute("gradeData", List.of());
            model.addAttribute("enrollmentLabels", List.of());
            model.addAttribute("enrollmentData", List.of());
            model.addAttribute("courseLabels", List.of());
            model.addAttribute("courseData", List.of());
        }

        // Admin total stats
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            model.addAttribute("totalStudents", adminService.getTotalStudents());
            model.addAttribute("totalLecturers", adminService.getTotalLecturers());
        } else {
            model.addAttribute("totalStudents", 0);
            model.addAttribute("totalLecturers", 0);
        }

        return "lecturer/reports";
    }

    private Long resolveLecturerId(Long paramId) {
        if (paramId != null) {
            return paramId;
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            return userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }

    private void populateCommonModel(Model model, Long lecturerId) {
        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("userId", lecturerId);
            model.addAttribute("role", "LECTURER");
        }
    }
}
