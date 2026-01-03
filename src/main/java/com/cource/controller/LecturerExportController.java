package com.cource.controller;

import com.cource.entity.CourseOffering;
import com.cource.entity.User;
import com.cource.service.LecturerService;
import com.cource.service.AdminService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/lecturer")
public class LecturerExportController {
    private final LecturerService lecturerService;
    private final AdminService adminService;

    public LecturerExportController(LecturerService lecturerService, AdminService adminService) {
        this.lecturerService = lecturerService;
        this.adminService = adminService;
    }

    @GetMapping("/courses/export")
    public void exportCourses(@RequestParam Long lecturerId, HttpServletResponse response) throws IOException {
        List<CourseOffering> offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
        String filename = "courses_lecturer_" + lecturerId + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        var writer = response.getWriter();
        writer.println("Offering ID,Course Code,Course Title,Term,Capacity,Active");
        for (CourseOffering off : offerings) {
            writer.printf("%d,%s,%s,%s,%d,%s\n",
                    off.getId(),
                    off.getCourse().getCourseCode(),
                    off.getCourse().getTitle().replaceAll(",", " "),
                    off.getTerm().getTermName(),
                    off.getCapacity(),
                    off.isActive() ? "Active" : "Inactive");
        }
        writer.flush();
    }

    @GetMapping("/students/export")
    public void exportStudents(@RequestParam Long offeringId, @RequestParam Long lecturerId,
            HttpServletResponse response) throws IOException {
        List<User> students = lecturerService.getEnrolledStudents(offeringId, lecturerId);
        String filename = "students_offering_" + offeringId + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        var writer = response.getWriter();
        writer.println("Student ID,First Name,Last Name,Email,Active");
        for (User s : students) {
            writer.printf("%d,%s,%s,%s,%s\n",
                    s.getId(),
                    s.getFirstName() != null ? s.getFirstName().replaceAll(",", " ") : "",
                    s.getLastName() != null ? s.getLastName().replaceAll(",", " ") : "",
                    s.getEmail() != null ? s.getEmail() : "",
                    s.isActive() ? "Active" : "Inactive");
        }
        writer.flush();
    }
}
