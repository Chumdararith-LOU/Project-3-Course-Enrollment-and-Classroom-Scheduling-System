package com.cource.controller;

import com.cource.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/enrollment")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    @PostMapping("/enroll")
    public String enroll(@RequestParam Long offeringId, RedirectAttributes redirectAttributes) {
        try {
            Long studentId = 3L;

            var result = enrollmentService.enrollStudent(studentId, offeringId);

            if (result.getStatus().equals("ENROLLED")) {
                redirectAttributes.addFlashAttribute("successMessage", result.getMessage());
            } else if (result.getStatus().equals("WAITLISTED")) {
                redirectAttributes.addFlashAttribute("warningMessage", result.getMessage());
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/catalog";
    }

    @PostMapping("/drop")
    public String drop(@RequestParam Long offeringId, RedirectAttributes redirectAttributes) {
        try {
            Long studentId = 3L;

            var result = enrollmentService.dropCourse(studentId, offeringId);
            redirectAttributes.addFlashAttribute("successMessage", result.getMessage());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/courses";
    }
}
