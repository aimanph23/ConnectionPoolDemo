package com.example.connectionpool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class JvmDashboardController {

    @GetMapping("/jvm")
    public String getJvmDashboard() {
        return "jvm-dashboard.html";
    }
}
