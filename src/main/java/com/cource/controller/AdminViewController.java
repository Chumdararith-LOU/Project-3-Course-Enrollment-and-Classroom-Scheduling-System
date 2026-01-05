package com.cource.controller;

import com.cource.entity.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cource.repository.RoleRepository;
import com.cource.service.AdminService;
import com.cource.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final AdminService adminService;
    private final UserService userService;
    private final RoleRepository roleRepository;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("totalStudents", adminService.getTotalStudents());
        model.addAttribute("totalLecturers", adminService.getTotalLecturers());
        model.addAttribute("totalCourses", adminService.getTotalCourses());
        model.addAttribute("totalEnrollments", adminService.getTotalEnrollments());
        var enrollmentMap = adminService.getEnrollmentStatsByTerm();
        var enrollmentLabels = new java.util.ArrayList<String>(enrollmentMap.keySet());
        var enrollmentData = new java.util.ArrayList<Number>();
        for (String k : enrollmentLabels) {
            Object v = enrollmentMap.get(k);
            try {
                enrollmentData.add((Number) v);
            } catch (Exception ex) {
                try {
                    enrollmentData.add(Long.parseLong(String.valueOf(v)));
                } catch (Exception e) {
                    enrollmentData.add(0);
                }
            }
        }
        model.addAttribute("enrollmentLabels", enrollmentLabels);
        model.addAttribute("enrollmentData", enrollmentData);

        var roles = roleRepository.findAll();
        var userLabels = new java.util.ArrayList<String>();
        var userData = new java.util.ArrayList<Number>();
        for (var r : roles) {
            long count = adminService.getUsersByRole(r.getRoleCode()).size();
            userLabels.add(r.getRoleName());
            userData.add(count);
        }
        model.addAttribute("userLabels", userLabels);
        model.addAttribute("userData", userData);

        addUserInfoToModel(userDetails, model);
        model.addAttribute("currentPage", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String roleCode,
            @AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (roleCode != null && !roleCode.isEmpty()) {
            model.addAttribute("users", adminService.getUsersByRole(roleCode));
            model.addAttribute("roleCode", roleCode);
        } else {
            model.addAttribute("users", adminService.getAllUsers());
        }
        model.addAttribute("roles", roleRepository.findAll());
        addUserInfoToModel(userDetails, model);
        model.addAttribute("currentPage", "users");
        return "admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.getUserById(id));
        model.addAttribute("roles", roleRepository.findAll());
        return "admin/user-edit";
    }

    @GetMapping("/courses")
    public String courses(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("courses", adminService.getAllCourses());
        model.addAttribute("lecturers", adminService.getUsersByRole("LECTURER"));
        addUserInfoToModel(userDetails, model);
        model.addAttribute("currentPage", "courses");
        return "admin/courses";
    }

    @GetMapping("/offerings")
    public String offerings(@RequestParam(required = false) Long termId,
            @AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("terms", adminService.getAllTerms());
        model.addAttribute("courses", adminService.getAllCourses());
        var lecturers = new java.util.ArrayList<>(adminService.getUsersByRole("LECTURER"));
        var admins = adminService.getUsersByRole("ADMIN");
        for (var admin : admins) {
            if (!lecturers.contains(admin)) {
                lecturers.add(admin);
            }
        }
        model.addAttribute("lecturers", lecturers);
        if (termId != null) {
            model.addAttribute("offerings", adminService.getCourseOfferingsByTerm(termId));
            model.addAttribute("termId", termId);
        } else {
            model.addAttribute("offerings", adminService.getAllCourseOfferings());
        }
        addUserInfoToModel(userDetails, model);
        model.addAttribute("currentPage", "offerings");
        return "admin/offerings";
    }

    @GetMapping("/rooms")
    public String rooms(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("rooms", adminService.getAllRooms());
        addUserInfoToModel(userDetails, model);
        model.addAttribute("currentPage", "rooms");
        return "admin/rooms";
    }

    @GetMapping("/schedules")
    public String schedules(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("schedules", adminService.getAllSchedules());
        model.addAttribute("offerings", adminService.getAllCourseOfferings());
        model.addAttribute("rooms", adminService.getAllRooms());
        addUserInfoToModel(userDetails, model);
        model.addAttribute("currentPage", "schedules");
        return "admin/schedules";
    }

    @GetMapping("/terms")
    public String terms(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("terms", adminService.getAllTerms());
        addUserInfoToModel(userDetails, model);
        model.addAttribute("currentPage", "terms");
        return "admin/terms";
    }

    @GetMapping("/enrollments")
    public String enrollments(@RequestParam(required = false) Long offeringId,
            @AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (offeringId != null) {
            model.addAttribute("enrollments", adminService.getEnrollmentsByOffering(offeringId));
            model.addAttribute("offeringId", offeringId);
        } else {
            model.addAttribute("enrollments", adminService.getAllEnrollments());
        }
        model.addAttribute("offerings", adminService.getAllCourseOfferings());
        model.addAttribute("students", adminService.getUsersByRole("STUDENT"));
        addUserInfoToModel(userDetails, model);
        model.addAttribute("currentPage", "enrollments");
        return "admin/enrollments";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("totalStudents", adminService.getTotalStudents());
        model.addAttribute("totalLecturers", adminService.getTotalLecturers());
        model.addAttribute("totalCourses", adminService.getTotalCourses());
        model.addAttribute("totalEnrollments", adminService.getTotalEnrollments());

        var enrollmentMap = adminService.getEnrollmentStatsByTerm();
        var enrollmentList = new java.util.ArrayList<java.util.Map.Entry<String, Object>>(enrollmentMap.entrySet());
        model.addAttribute("enrollmentStats", enrollmentList);

        // Also provide arrays for charts
        var enrollmentLabels = new java.util.ArrayList<String>(enrollmentMap.keySet());
        var enrollmentData = new java.util.ArrayList<Number>();
        for (String k : enrollmentLabels) {
            Object v = enrollmentMap.get(k);
            try {
                enrollmentData.add((Number) v);
            } catch (Exception ex) {
                try {
                    enrollmentData.add(Long.parseLong(String.valueOf(v)));
                } catch (Exception e) {
                    enrollmentData.add(0);
                }
            }
        }
        model.addAttribute("enrollmentLabels", enrollmentLabels);
        model.addAttribute("enrollmentData", enrollmentData);

        // User distribution by role
        var roles = roleRepository.findAll();
        var userLabels = new java.util.ArrayList<String>();
        var userData = new java.util.ArrayList<Number>();
        for (var r : roles) {
            long count = adminService.getUsersByRole(r.getRoleCode()).size();
            userLabels.add(r.getRoleName());
            userData.add(count);
        }
        model.addAttribute("userLabels", userLabels);
        model.addAttribute("userData", userData);

        var courseMap = adminService.getCoursePopularity();
        var courseList = new java.util.ArrayList<java.util.Map.Entry<String, Object>>(courseMap.entrySet());
        model.addAttribute("coursePopularity", courseList);
        double courseMax = courseList.stream()
                .mapToDouble(e -> {
                    try {
                        return Double.parseDouble(String.valueOf(e.getValue()));
                    } catch (Exception ex) {
                        return 0.0;
                    }
                })
                .max()
                .orElse(1.0);
        model.addAttribute("courseMax", courseMax);
        return "admin/reports";
    }

    @GetMapping("/users/export")
    public ResponseEntity<String> exportUsers(@RequestParam(required = false) String roleCode) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Email,First Name,Last Name,ID Card,Role,Status\n");

        var users = roleCode != null && !roleCode.isEmpty()
                ? adminService.getUsersByRole(roleCode)
                : adminService.getAllUsers();

        for (var user : users) {
            csv.append(String.format("%d,%s,%s,%s,%s,%s,%s\n",
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getIdCard(),
                    user.getRole().getRoleCode(),
                    user.isActive() ? "Active" : "Inactive"));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users-export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    @GetMapping("/courses/export")
    public ResponseEntity<String> exportCourses() {
        StringBuilder csv = new StringBuilder();
        csv.append("Course Code,Title,Description,Credits,Status\n");

        for (var course : adminService.getAllCourses()) {
            csv.append(String.format("%s,%s,%s,%d,%s\n",
                    course.getCourseCode(),
                    course.getTitle(),
                    course.getDescription() != null ? course.getDescription().replace(",", ";") : "",
                    course.getCredits(),
                    course.isActive() ? "Active" : "Inactive"));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=courses-export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    @GetMapping("/offerings/export")
    public ResponseEntity<String> exportOfferings(@RequestParam(required = false) Long termId) {
        StringBuilder csv = new StringBuilder();
        csv.append("Offering ID,Course Code,Course Title,Term,Capacity,Enrolled,Available,Status\n");

        var offerings = termId != null
                ? adminService.getCourseOfferingsByTerm(termId)
                : adminService.getAllCourseOfferings();

        for (var offering : offerings) {
            // Get enrollment count (you might need to add this to AdminService)
            var enrollments = adminService.getEnrollmentsByOffering(offering.getId());
            int enrolledCount = enrollments.size();
            int available = offering.getCapacity() - enrolledCount;

            csv.append(String.format("%d,%s,%s,%s,%d,%d,%d,%s\n",
                    offering.getId(),
                    offering.getCourse().getCourseCode(),
                    offering.getCourse().getTitle(),
                    offering.getTerm().getTermName(),
                    offering.getCapacity(),
                    enrolledCount,
                    available,
                    offering.isActive() ? "Active" : "Inactive"));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=offerings-export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    @GetMapping("/enrollments/export")
    public ResponseEntity<String> exportEnrollments(@RequestParam(required = false) Long offeringId) {
        StringBuilder csv = new StringBuilder();
        csv.append("Student ID,Student Name,Email,Course,Term,Status,Grade\n");

        var enrollments = offeringId != null
                ? adminService.getEnrollmentsByOffering(offeringId)
                : adminService.getAllEnrollments();

        for (var enrollment : enrollments) {
            csv.append(String.format("%s,%s %s,%s,%s,%s,%s,%s\n",
                    enrollment.getStudent().getIdCard(),
                    enrollment.getStudent().getFirstName(),
                    enrollment.getStudent().getLastName(),
                    enrollment.getStudent().getEmail(),
                    enrollment.getOffering().getCourse().getTitle(),
                    enrollment.getOffering().getTerm().getTermCode(),
                    enrollment.getStatus(),
                    enrollment.getGrade() != null ? enrollment.getGrade() : "N/A"));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enrollments-export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    @GetMapping("/rooms/export")
    public ResponseEntity<String> exportRooms() {
        StringBuilder csv = new StringBuilder();
        csv.append("Room Number,Building,Capacity,Type,Status\n");

        for (var room : adminService.getAllRooms()) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                    room.getRoomNumber(),
                    room.getBuilding(),
                    room.getCapacity(),
                    room.getRoomType() != null ? room.getRoomType() : "N/A",
                    room.isActive() ? "Active" : "Inactive"));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rooms-export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    @GetMapping("/activities")
    public String activities(Model model) {
        // This would show system activity logs
        // For now, redirect to dashboard
        return "redirect:/admin/dashboard";
    }

    // Helper method to add user info to model
    private void addUserInfoToModel(UserDetails userDetails, Model model) {
        model.addAttribute("userId", 1L);
        model.addAttribute("role", "ADMIN");

        if (userDetails != null) {
            try {
                User user = userService.getUserByEmail(userDetails.getUsername());
                if (user != null) {
                    model.addAttribute("userId", user.getId());
                    if (user.getRole() != null) {
                        model.addAttribute("role", user.getRole().getRoleCode());
                    }
                }
            } catch (Exception e) {
                // Use defaults if unable to fetch user
            }
        }
    }
}