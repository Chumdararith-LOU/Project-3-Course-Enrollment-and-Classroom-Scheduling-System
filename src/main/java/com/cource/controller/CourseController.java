package com.cource.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import com.cource.service.CourseService;

@RestController
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

}
