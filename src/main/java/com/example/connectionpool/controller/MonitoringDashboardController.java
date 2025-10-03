package com.example.connectionpool.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class MonitoringDashboardController {

    /**
     * Serves the real-time monitoring dashboard HTML page
     * 
     * Access: http://localhost:8080/dashboard/hikari
     */
    @GetMapping("/hikari")
    public String hikariDashboard() {
        log.info("Serving HikariCP real-time monitoring dashboard");
        return "hikari-dashboard";
    }
}

