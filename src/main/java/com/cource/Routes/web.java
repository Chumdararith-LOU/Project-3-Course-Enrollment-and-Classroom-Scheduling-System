package com.cource.Routes;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import ch.qos.logback.core.model.Model;


@Controller
public class web {

    @GetMapping("/")
    public String home(Model model) {
        return "index";
    }

    // // view dashboard for lecturer including courses, schedules, and students
    // @GetMapping("/lecturer/dashboard/{lecturerId}")
    // public String lecturerDashboard(@PathVariable Long lecturerId, Model model) {
    //     model.addAttribute("lecturerId", lecturerId);
    //     return "dashboard";
    // }

    // // view course details including schedules and enrolled students
    // @GetMapping("/lecturer/course/{courseId}")
    // public String courseDetails(@PathVariable Long courseId, @RequestParam Long lecturerId, Model model) {
    //     model.addAttribute("courseId", courseId);
    //     model.addAttribute("lecturerId", lecturerId);
    //     return "courses";
    // }

    
}
