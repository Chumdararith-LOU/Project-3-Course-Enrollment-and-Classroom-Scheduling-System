package com.cource.controller.Student;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cource.service.StudentService;
import com.cource.util.SecurityHelper;

import org.springframework.security.access.prepost.PreAuthorize;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final StudentService studentService;
    private final SecurityHelper securityHelper;

    @GetMapping("/student/drop")
    public String dropEnrollment(@RequestParam Long enrollmentId, RedirectAttributes ra) {
        Long currentUserId = securityHelper.getCurrentUserId();
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "Unable to resolve current user");
            return "redirect:/login";
        }
        try {
            studentService.dropEnrollment(currentUserId, enrollmentId);
            ra.addFlashAttribute("message", "Course dropped successfully");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/dashboard?studentId=" + currentUserId;
    }

    @GetMapping("/student/re-enroll")
    public String restoreEnrollment(@RequestParam Long enrollmentId, RedirectAttributes ra) {
        Long currentUserId = securityHelper.getCurrentUserId();
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "Unable to resolve current user");
            return "redirect:/login";
        }
        try {
            studentService.restoreEnrollment(currentUserId, enrollmentId);
            ra.addFlashAttribute("message", "Enrollment restored successfully");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/dashboard?studentId=" + currentUserId;
    }

    @GetMapping("/student/delete-enrollment")
    public String deleteEnrollment(@RequestParam Long enrollmentId, RedirectAttributes ra) {
        Long currentUserId = securityHelper.getCurrentUserId();
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "Unable to resolve current user");
            return "redirect:/login";
        }
        try {
            studentService.deleteEnrollment(currentUserId, enrollmentId);
            ra.addFlashAttribute("message", "Enrollment deleted successfully");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/dashboard?studentId=" + currentUserId;
    }

    @PostMapping("/student/attendance/submit")
    public String submitAttendance(@RequestParam Long scheduleId, @RequestParam String attendanceDate,
            @RequestParam(required = false) String notes, RedirectAttributes ra) {
        Long currentUserId = securityHelper.getCurrentUserId();
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "Unable to resolve current user");
            return "redirect:/login";
        }
        com.cource.entity.Attendance saved = null;
        try {
            LocalDate date = LocalDate.parse(attendanceDate);
            saved = studentService.submitAttendanceRequest(currentUserId, scheduleId, date, notes);
            ra.addFlashAttribute("message", "Attendance request submitted");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        Long offeringId = null;
        if (saved != null && saved.getSchedule() != null && saved.getSchedule().getOffering() != null) {
            offeringId = saved.getSchedule().getOffering().getId();
        }
        return "redirect:/student/attendance?studentId=" + currentUserId
                + (offeringId != null ? "&offeringId=" + offeringId : "");
    }

}
