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

            enrollmentService.enrollStudent(studentId, offeringId);

            redirectAttributes.addFlashAttribute("successMessage", "Successfully enrolled in course!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/catalog";
    }
}
