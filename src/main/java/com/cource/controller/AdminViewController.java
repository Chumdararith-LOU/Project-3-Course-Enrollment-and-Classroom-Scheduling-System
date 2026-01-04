package com.cource.controller;

import com.cource.entity.User;
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
    public String dashboard(Model model) {
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

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String roleCode, Model model) {
        if (roleCode != null && !roleCode.isEmpty()) {
            model.addAttribute("users", adminService.getUsersByRole(roleCode));
            model.addAttribute("roleCode", roleCode);
        } else {
            model.addAttribute("users", adminService.getAllUsers());
        }
        model.addAttribute("roles", roleRepository.findAll());
        return "admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.getUserById(id));
        model.addAttribute("roles", roleRepository.findAll());
        return "views/admin/user-edit";
    }

    @GetMapping("/courses")
    public String courses(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("courses", adminService.getAllCourses());
        model.addAttribute("lecturers", adminService.getUsersByRole("LECTURER"));

        // Add user info for sidebar - set defaults if not available
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
        return "admin/courses";
    }

    @GetMapping("/offerings")
    public String offerings(@RequestParam(required = false) Long termId, Model model) {
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
        return "admin/offerings";
    }

    @GetMapping("/rooms")
    public String rooms(Model model) {
        model.addAttribute("rooms", adminService.getAllRooms());
        return "admin/rooms";
    }

    @GetMapping("/schedules")
    public String schedules(Model model) {
        model.addAttribute("schedules", adminService.getAllSchedules());
        model.addAttribute("offerings", adminService.getAllCourseOfferings());
        model.addAttribute("rooms", adminService.getAllRooms());
        return "admin/schedules";
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("terms", adminService.getAllTerms());
        return "admin/terms";
    }

    @GetMapping("/enrollments")
    public String enrollments(@RequestParam(required = false) Long offeringId, Model model) {
        if (offeringId != null) {
            model.addAttribute("enrollments", adminService.getEnrollmentsByOffering(offeringId));
            model.addAttribute("offeringId", offeringId);
        } else {
            model.addAttribute("enrollments", adminService.getAllEnrollments());
        }
        model.addAttribute("offerings", adminService.getAllCourseOfferings());
        model.addAttribute("students", adminService.getUsersByRole("STUDENT"));
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

    @GetMapping("/activities")
    public String activities(Model model) {
        return "redirect:/admin/dashboard";
    }
}