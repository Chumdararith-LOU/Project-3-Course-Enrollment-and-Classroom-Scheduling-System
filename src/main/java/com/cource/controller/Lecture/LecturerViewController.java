package com.cource.controller.Lecture;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cource.service.LecturerService;
import com.cource.service.AdminService;
import com.cource.repository.RoleRepository;
import com.cource.repository.ClassScheduleRepository;
import com.cource.repository.EnrollmentRepository;

@Controller
@RequestMapping("/lecturer")
@PreAuthorize("hasRole('LECTURER')")
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
                                    Locale.ENGLISH).toUpperCase(); // e.g., Mon -> MON
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
            var enrollmentLabels = new ArrayList<String>(attendanceMap.keySet());
            var enrollmentData = new ArrayList<Number>();
            for (String k : enrollmentLabels) {
                enrollmentData.add(attendanceMap.getOrDefault(k, 0L));
            }
            model.addAttribute("enrollmentLabels", enrollmentLabels);
            model.addAttribute("enrollmentData", enrollmentData);

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
        } else {
            model.addAttribute("enrollmentLabels", new ArrayList<String>());
            model.addAttribute("enrollmentData", new ArrayList<Number>());
            model.addAttribute("userLabels", new ArrayList<String>());
            model.addAttribute("userData", new ArrayList<Number>());
        }
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
                model.addAttribute("courses", adminService.getAllCourses());
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

                lecturerService.getEnrolledStudents(offeringId, lecturerId);

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
                model.addAttribute("schedules", new ArrayList<>());
            }
        }

        if (studentId != null) {
            model.addAttribute("initialStudentId", studentId);
        } else {
            model.addAttribute("initialStudentId", null);
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
                    var students = lecturerService.getEnrolledStudents(offId, lecturerId);
                    model.addAttribute("students", students);
                } else {
                    model.addAttribute("students", new ArrayList<>());
                }
            } catch (Exception ex) {
                model.addAttribute("students", new ArrayList<>());
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
            System.out.println("[DEBUG] Offerings for lecturerId=" + lecturerId + ":");
            for (var off : offerings) {
                System.out.println("  OfferingId=" + off.getId() + ", Course=" + off.getCourse().getCourseCode() + " - "
                        + off.getCourse().getTitle() + ", Term=" + off.getTerm().getTermName());
            }
            List<com.cource.entity.ClassSchedule> schedules = new ArrayList<>();
            for (var offering : offerings) {
                var classSchedules = classScheduleRepository.findByOfferingId(offering.getId());
                System.out.println("    Schedules for OfferingId=" + offering.getId() + ": "
                        + (classSchedules != null ? classSchedules.size() : 0));
                if (classSchedules != null) {
                    for (var sched : classSchedules) {
                        System.out.println("      ScheduleId=" + sched.getId() + ", Day=" + sched.getDayOfWeek()
                                + ", Start=" + sched.getStartTime() + ", End=" + sched.getEndTime());
                    }
                    schedules.addAll(classSchedules);
                }
            }
            System.out.println("[DEBUG] Total schedules found: " + schedules.size());
            model.addAttribute("schedules", schedules);
            model.addAttribute("offerings", offerings); // <-- Add offerings for course selection
        } else {
            model.addAttribute("offerings", new ArrayList<>());
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
            model.addAttribute("terms", new ArrayList<>());
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
                model.addAttribute("gradeLabels", new ArrayList<String>());
                model.addAttribute("gradeData", new ArrayList<Number>());
            }
        } else {
            model.addAttribute("offerings", new ArrayList<>());
            model.addAttribute("summaryAvgAttendance", 0.0);
            model.addAttribute("summaryPassRate", 0.0);
            model.addAttribute("summaryActiveEnrollments", 0L);
            model.addAttribute("summaryTotalClasses", 0L);
            model.addAttribute("courseReports", new ArrayList<>());
            model.addAttribute("courseDetail", null);
            model.addAttribute("gradeLabels", new ArrayList<String>());
            model.addAttribute("gradeData", new ArrayList<Number>());
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
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
            model.addAttribute("enrollmentLabels", new ArrayList<String>());
            model.addAttribute("enrollmentData", new ArrayList<Number>());
            model.addAttribute("courseLabels", new ArrayList<String>());
            model.addAttribute("courseData", new ArrayList<Number>());
        }
        return "lecturer/reports";
    }
}
