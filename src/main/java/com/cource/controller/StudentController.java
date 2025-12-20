package com.cource.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        
        return "student/dashboard";
    }

    @GetMapping("/catalog")
    public String courseCatalog(Model model) {
    
        return "student/catalog";
    }

    
    @GetMapping("/my-courses")
    public String myCourses(Model model) {
       
        return "student/my-courses";
    }

    
    @GetMapping("/schedule")
    public String schedule() {
        // The schedule grid is complex to render dynamically. 
        // For now, we usually return the static view, or a list of time slots if you get advanced.
        return "student/schedule";
    }

   
    @GetMapping("/grades")
    public String grades(Model model) {

        return "student/grades";
    }


    @GetMapping("/attendance")
    public String attendance(Model model) {
        
        return "student/attendance";
    }

}