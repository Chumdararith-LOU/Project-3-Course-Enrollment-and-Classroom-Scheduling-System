package com.cource.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cource.service.LecturerService;
import com.cource.service.AdminService;
import com.cource.repository.RoleRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.EnrollmentRepository;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Controller for rendering Lecturer HTML views (Thymeleaf templates)
 * Separate from REST API endpoints in LecturerController
 */
@Controller
@RequestMapping("/lecturer")
public class LecturerViewController {

    private final LecturerService lecturerService;
    private final AdminService adminService;
    private final RoleRepository roleRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;

    public LecturerViewController(LecturerService lecturerService, AdminService adminService,
            RoleRepository roleRepository, ClassScheduleRepository classScheduleRepository,
            EnrollmentRepository enrollmentRepository) {
        this.lecturerService = lecturerService;
        this.adminService = adminService;
        this.roleRepository = roleRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Render lecturer dashboard page
     */
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long lecturerId, Model model) {
        // TODO: After enabling security, get lecturerId from Authentication
        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
            // You can fetch dashboard data here if needed
            // model.addAttribute("courses",
            // lecturerService.getCoursesByLecturerId(lecturerId));
        }

        // Dashboard totals for the lecturer: total courses, total students, classes
        // today
        if (lecturerId != null) {
            var offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
            int totalCourses = offerings == null ? 0 : offerings.size();
            long totalStudents = 0;
            int classesToday = 0;
            var today = java.time.LocalDate.now();
            if (offerings != null) {
                for (var off : offerings) {
                    try {
                        var students = lecturerService.getEnrolledStudents(off.getId(), lecturerId);
                        if (students != null)
                            totalStudents += students.size();
                    } catch (Exception ignored) {
                    }
                    try {
                        var schedules = lecturerService.getClassSchedulesByLecturerId(off.getId(), lecturerId);
                        if (schedules != null) {
                            java.time.DayOfWeek dow = today.getDayOfWeek();
                            String dowFull = dow.name(); // e.g., MONDAY
                            String dowShort = dowFull.substring(0, 3); // e.g., MON
                            String dowShortDisplay = dow.getDisplayName(java.time.format.TextStyle.SHORT,
                                    java.util.Locale.ENGLISH).toUpperCase(); // e.g., Mon -> MON
                            for (var s : schedules) {
                                String schedDow = s.getDayOfWeek();
                                if (schedDow == null)
                                    continue;
                                String sd = schedDow.trim().toUpperCase();
                                if (sd.equals(dowFull) || sd.equals(dowShort) || sd.equals(dowShortDisplay)) {
                                    classesToday++;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            model.addAttribute("totalCourses", totalCourses);
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("classesToday", classesToday);
        }
        // Provide chart data using LecturerService: recent attendance (7 days) + role
        // distribution
        // Only fetch system-wide admin metrics when the current user has ADMIN role
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        if (auth != null && auth.getAuthorities() != null) {
            isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        if (isAdmin) {
            model.addAttribute("systemTotalStudents", adminService.getTotalStudents());
            model.addAttribute("systemTotalLecturers", adminService.getTotalLecturers());
        } else {
            model.addAttribute("systemTotalStudents", 0);
            model.addAttribute("systemTotalLecturers", 0);
        }

        if (lecturerId != null) {
            var attendanceMap = lecturerService.getAttendanceCountsByDate(lecturerId, 7);
            var enrollmentLabels = new java.util.ArrayList<String>(attendanceMap.keySet());
            var enrollmentData = new java.util.ArrayList<Number>();
            for (String k : enrollmentLabels) {
                enrollmentData.add(attendanceMap.getOrDefault(k, 0L));
            }
            model.addAttribute("enrollmentLabels", enrollmentLabels);
            model.addAttribute("enrollmentData", enrollmentData);

            var roles = roleRepository.findAll();
            var userLabels = new java.util.ArrayList<String>();
            var userData = new java.util.ArrayList<Number>();
            if (isAdmin) {
                for (var r : roles) {
                    long count = adminService.getUsersByRole(r.getRoleCode()).size();
                    userLabels.add(r.getRoleName());
                    userData.add(count);
                }
            } else {
                // For non-admin lecturers, avoid calling adminService (method-secured)
                // Provide empty data or zeros so the dashboard renders safely.
                for (var r : roles) {
                    userLabels.add(r.getRoleName());
                    userData.add(0);
                }
            }
            model.addAttribute("userLabels", userLabels);
            model.addAttribute("userData", userData);
        } else {
            model.addAttribute("enrollmentLabels", new java.util.ArrayList<String>());
            model.addAttribute("enrollmentData", new java.util.ArrayList<Number>());
            model.addAttribute("userLabels", new java.util.ArrayList<String>());
            model.addAttribute("userData", new java.util.ArrayList<Number>());
        }
        return "lecturer/dashboard";
    }

    /**
     * Render courses page
     */
    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) Long lecturerId, Model model) {
        // TODO: After enabling security, get lecturerId from Authentication
        try {
            if (lecturerId != null) {
                model.addAttribute("lecturerId", lecturerId);
                model.addAttribute("offerings", lecturerService.getOfferingsByLecturerId(lecturerId));
            }
            // Only fetch admin-level lists if the current user has ADMIN role
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = false;
            if (auth != null && auth.getAuthorities() != null) {
                isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            }
            if (isAdmin) {
                model.addAttribute("courses", adminService.getAllCourses());
                model.addAttribute("terms", adminService.getAllTerms());
            } else {
                model.addAttribute("courses", new java.util.ArrayList<>());
                model.addAttribute("terms", new java.util.ArrayList<>());
            }
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
        }
        return "lecturer/courses";
    }

    /**
     * Render students page
     */
    @GetMapping("/students")
    public String students(
            @RequestParam(required = false) Long offeringId,
            @RequestParam(required = false) Long lecturerId,
            Model model) {
        // TODO: After enabling security, get lecturerId from Authentication
        if (offeringId != null && lecturerId != null) {
            try {
                model.addAttribute("offeringId", offeringId);
                model.addAttribute("lecturerId", lecturerId);
                // Verify lecturer owns this offering (throws if not)
                lecturerService.getEnrolledStudents(offeringId, lecturerId);

                // Load enrollments + students for this offering (ENROLLED only)
                var enrollments = enrollmentRepository.findByOfferingIdWithStudentFiltered(offeringId, "ENROLLED");
                model.addAttribute("enrollments", enrollments);

                java.util.List<com.cource.entity.Student> students = new java.util.ArrayList<>();
                java.util.Map<Long, com.cource.entity.Enrollment> enrollmentMap = new java.util.HashMap<>();
                if (enrollments != null) {
                    for (com.cource.entity.Enrollment e : enrollments) {
                        if (e != null && e.getStudent() != null && e.getStudent().getId() != null) {
                            students.add(e.getStudent());
                            enrollmentMap.put(e.getStudent().getId(), e);
                        }
                    }
                }

                System.out.println("[DEBUG] LecturerViewController.students offeringId=" + offeringId
                        + ", enrollmentsCount=" + (enrollments == null ? 0 : enrollments.size())
                        + ", studentsCount=" + (students == null ? 0 : students.size()));

                model.addAttribute("students", students);
                model.addAttribute("enrollmentMap", enrollmentMap);
            } catch (Exception ex) {
                model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
            }
        }
        return "lecturer/students";
    }

    /**
     * Render attendance page
     */
    @GetMapping("/attendance")
    public String attendance(
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) Long lecturerId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long offeringId,
            Model model) {
        // TODO: After enabling security, get lecturerId from Authentication
        // If lecturer provided but no schedule selected, try to auto-select the first
        // schedule for one of the lecturer's offerings so the page shows useful data
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
            } catch (Exception ignored) {
            }
        }
        // Provide schedules for the lecturer so the template can show a selector
        if (lecturerId != null) {
            try {
                java.util.List<com.cource.entity.ClassSchedule> schedules = new java.util.ArrayList<>();
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
                model.addAttribute("schedules", new java.util.ArrayList<>());
            }
        }

        // Pass through an initial studentId (if any) so the attendance modal can
        // preselect
        if (studentId != null) {
            model.addAttribute("initialStudentId", studentId);
        } else {
            model.addAttribute("initialStudentId", null);
        }
        // Also provide offeringId if supplied to help auto-select schedules
        if (offeringId != null) {
            model.addAttribute("offeringId", offeringId);
        }

        if (scheduleId != null && lecturerId != null) {
            model.addAttribute("scheduleId", scheduleId);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("attendanceRecords", lecturerService.getAttendanceRecords(scheduleId, lecturerId));
            // Also load enrolled students for this schedule's offering so lecturer can mark
            // attendance
            try {
                var schedOpt = classScheduleRepository.findById(scheduleId);
                if (schedOpt.isPresent() && schedOpt.get().getOffering() != null) {
                    Long offId = schedOpt.get().getOffering().getId();
                    var students = lecturerService.getEnrolledStudents(offId, lecturerId);
                    model.addAttribute("students", students);
                } else {
                    model.addAttribute("students", new java.util.ArrayList<>());
                }
            } catch (Exception ex) {
                model.addAttribute("students", new java.util.ArrayList<>());
            }
        }
        return "lecturer/attendance";
    }

    /**
     * Render schedule page
     */
    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
            // Fetch all offerings for this lecturer
            var offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
            System.out.println("[DEBUG] Offerings for lecturerId=" + lecturerId + ":");
            for (var off : offerings) {
                System.out.println("  OfferingId=" + off.getId() + ", Course=" + off.getCourse().getCourseCode() + " - "
                        + off.getCourse().getTitle() + ", Term=" + off.getTerm().getTermName());
            }
            java.util.List<com.cource.entity.ClassSchedule> schedules = new java.util.ArrayList<>();
            for (var offering : offerings) {
                var classSchedules = classScheduleRepository.findByOfferingId(offering.getId());
                System.out.println("    Schedules for OfferingId=" + offering.getId() + ": "
                        + (classSchedules != null ? classSchedules.size() : 0));
                for (var sched : classSchedules) {
                    System.out.println("      ScheduleId=" + sched.getId() + ", Day=" + sched.getDayOfWeek()
                            + ", Start=" + sched.getStartTime() + ", End=" + sched.getEndTime());
                }
                schedules.addAll(classSchedules);
            }
            System.out.println("[DEBUG] Total schedules found: " + schedules.size());
            model.addAttribute("schedules", schedules);
            model.addAttribute("offerings", offerings); // <-- Add offerings for course selection
        } else {
            model.addAttribute("offerings", new java.util.ArrayList<>());
        }
        // Always provide all terms for the modal
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        if (auth != null && auth.getAuthorities() != null) {
            isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        if (isAdmin) {
            model.addAttribute("terms", adminService.getAllTerms());
        } else {
            model.addAttribute("terms", new java.util.ArrayList<>());
        }
        return "lecturer/schedule";
    }

    /**
     * Render reports page
     */
    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false) Long lecturerId,
            @RequestParam(required = false) Long offeringId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String studentStatus,
            Model model) {
        // TODO: After enabling security, get lecturerId from Authentication
        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
        }

        java.time.LocalDate fromDate;
        java.time.LocalDate toDate;
        try {
            fromDate = (from == null || from.isBlank()) ? java.time.LocalDate.now().minusDays(29)
                    : java.time.LocalDate.parse(from);
        } catch (Exception ex) {
            fromDate = java.time.LocalDate.now().minusDays(29);
        }
        try {
            toDate = (to == null || to.isBlank()) ? java.time.LocalDate.now() : java.time.LocalDate.parse(to);
        } catch (Exception ex) {
            toDate = java.time.LocalDate.now();
        }
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

            // summary cards (lecturer-only)
            double avgAttendance = lecturerService.calculateAverageAttendance(lecturerId, fromDate, toDate, offeringId,
                    studentStatus);
            model.addAttribute("summaryAvgAttendance", avgAttendance);

            double passRate;
            if (offeringId != null) {
                passRate = lecturerService.calculatePassRate(lecturerId, offeringId, studentStatus);
            } else {
                // weighted pass rate across offerings
                double sum = 0.0;
                int n = 0;
                for (var off : offerings) {
                    sum += lecturerService.calculatePassRate(lecturerId, off.getId(), studentStatus);
                    n++;
                }
                passRate = n == 0 ? 0.0 : (sum / n);
            }
            model.addAttribute("summaryPassRate", passRate);

            java.util.List<Long> offeringIds = offerings == null ? java.util.List.of()
                    : offerings.stream().map(o -> o.getId()).toList();
            if (offeringId != null) {
                offeringIds = java.util.List.of(offeringId);
            }
            String statusToCount = (studentStatus == null || studentStatus.isBlank()) ? "ENROLLED" : studentStatus;
            long activeEnrollments = offeringIds.isEmpty() ? 0
                    : enrollmentRepository.countByOfferingIdsAndStatus(offeringIds, statusToCount);
            model.addAttribute("summaryActiveEnrollments", activeEnrollments);

            long totalClasses = offeringIds.isEmpty() ? 0 : classScheduleRepository.countByOfferingIds(offeringIds);
            model.addAttribute("summaryTotalClasses", totalClasses);

            // table data
            model.addAttribute("courseReports", lecturerService.getCourseReports(lecturerId, fromDate, toDate,
                    studentStatus));

            // optional details when a course is selected
            if (offeringId != null) {
                var detail = lecturerService.getDetailedCourseReport(lecturerId, offeringId, fromDate, toDate,
                        studentStatus);
                model.addAttribute("courseDetail", detail);

                // grade distribution chart arrays
                var gradeLabels = new java.util.ArrayList<String>();
                var gradeData = new java.util.ArrayList<Number>();
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
                model.addAttribute("gradeLabels", new java.util.ArrayList<String>());
                model.addAttribute("gradeData", new java.util.ArrayList<Number>());
            }
        } else {
            model.addAttribute("offerings", new java.util.ArrayList<>());
            model.addAttribute("summaryAvgAttendance", 0.0);
            model.addAttribute("summaryPassRate", 0.0);
            model.addAttribute("summaryActiveEnrollments", 0L);
            model.addAttribute("summaryTotalClasses", 0L);
            model.addAttribute("courseReports", new java.util.ArrayList<>());
            model.addAttribute("courseDetail", null);
            model.addAttribute("gradeLabels", new java.util.ArrayList<String>());
            model.addAttribute("gradeData", new java.util.ArrayList<Number>());
        }
        // Provide chart data specific to lecturer: attendance (30 days) and course
        // performance. Only include global admin metrics for users with ADMIN role.
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        if (auth != null && auth.getAuthorities() != null) {
            isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
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
            var attendanceLabels = new java.util.ArrayList<String>(attendanceMap.keySet());
            var attendanceData = new java.util.ArrayList<Number>();
            for (String k : attendanceLabels) {
                attendanceData.add(attendanceMap.getOrDefault(k, 0L));
            }
            model.addAttribute("enrollmentLabels", attendanceLabels);
            model.addAttribute("enrollmentData", attendanceData);

            var perf = lecturerService.getCourseAverageGradeByLecturer(lecturerId);
            var courseLabels = new java.util.ArrayList<String>(perf.keySet());
            var courseData = new java.util.ArrayList<Number>();
            for (String k : courseLabels) {
                courseData.add(perf.getOrDefault(k, 0.0));
            }
            model.addAttribute("courseLabels", courseLabels);
            model.addAttribute("courseData", courseData);
        } else {
            model.addAttribute("enrollmentLabels", new java.util.ArrayList<String>());
            model.addAttribute("enrollmentData", new java.util.ArrayList<Number>());
            model.addAttribute("courseLabels", new java.util.ArrayList<String>());
            model.addAttribute("courseData", new java.util.ArrayList<Number>());
        }
        return "lecturer/reports";
    }
}
