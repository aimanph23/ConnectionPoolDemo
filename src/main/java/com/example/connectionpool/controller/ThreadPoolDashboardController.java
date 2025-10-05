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
public class ThreadPoolDashboardController {

    /**
     * Serves the real-time thread pool monitoring dashboard HTML page
     * 
     * Access: http://localhost:8080/dashboard/threadpool
     */
    @GetMapping("/threadpool")
    public String threadPoolDashboard() {
        log.info("Serving Thread Pool real-time monitoring dashboard");
        return "threadpool-dashboard";
    }
}

