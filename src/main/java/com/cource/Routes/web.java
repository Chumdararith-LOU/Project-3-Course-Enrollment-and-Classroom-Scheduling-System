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
    
    
}
