package com.cource.controller.Student;

import com.cource.entity.User;
import com.cource.service.EnrollmentService;
import com.cource.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/enrollment")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    @PostMapping("/enroll")
    public String enroll(@RequestParam Long offeringId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            User student = userService.getUserByEmail(userDetails.getUsername());

            var result = enrollmentService.enrollStudent(student.getId(), offeringId);

            if ("ENROLLED".equals(result.getStatus())) {
                redirectAttributes.addFlashAttribute("successMessage", result.getMessage());
            } else if ("WAITLISTED".equals(result.getStatus())) {
                redirectAttributes.addFlashAttribute("warningMessage", result.getMessage());
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/catalog";
    }

    @PostMapping("/drop")
    public String drop(@RequestParam Long offeringId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            User student = userService.getUserByEmail(userDetails.getUsername());

            var result = enrollmentService.dropCourse(student.getId(), offeringId);
            redirectAttributes.addFlashAttribute("successMessage", result.getMessage());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/my-courses";
    }

    @PostMapping("/join")
    public String joinByCode(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String code,
            RedirectAttributes redirectAttributes) {
        try {
            User student = userService.getUserByEmail(userDetails.getUsername());
            var result = enrollmentService.enrollByCode(student.getId(), code);

            redirectAttributes.addFlashAttribute("successMessage", result.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/catalog";
    }
}
