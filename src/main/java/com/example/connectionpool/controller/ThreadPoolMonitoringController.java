package com.example.connectionpool.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/threadpool")
@Slf4j
public class ThreadPoolMonitoringController {

    private final ThreadPoolTaskExecutor taskExecutor;
    
    public ThreadPoolMonitoringController(@Qualifier("taskExecutor") Executor taskExecutor) {
        this.taskExecutor = (ThreadPoolTaskExecutor) taskExecutor;
    }
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Get thread pool metrics
     * 
     * Example: GET http://localhost:8080/api/threadpool/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getThreadPoolMetrics() {
        log.info("Fetching thread pool metrics");
        
        try {
            ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("poolName", "TaskExecutor");
            metrics.put("corePoolSize", executor.getCorePoolSize());
            metrics.put("maximumPoolSize", executor.getMaximumPoolSize());
            metrics.put("activeCount", executor.getActiveCount());
            metrics.put("poolSize", executor.getPoolSize());
            metrics.put("largestPoolSize", executor.getLargestPoolSize());
            metrics.put("taskCount", executor.getTaskCount());
            metrics.put("completedTaskCount", executor.getCompletedTaskCount());
            metrics.put("queueSize", executor.getQueue().size());
            metrics.put("queueRemainingCapacity", executor.getQueue().remainingCapacity());
            metrics.put("queueCapacity", executor.getQueue().size() + executor.getQueue().remainingCapacity());
            metrics.put("isShutdown", executor.isShutdown());
            metrics.put("isTerminated", executor.isTerminated());
            metrics.put("isTerminating", executor.isTerminating());
            metrics.put("timestamp", System.currentTimeMillis());
            
            // Calculate utilization percentages
            int utilizationPercent = executor.getMaximumPoolSize() > 0 
                ? (int) ((executor.getActiveCount() * 100.0) / executor.getMaximumPoolSize())
                : 0;
            metrics.put("utilizationPercent", utilizationPercent);
            
            int queueUtilizationPercent = (executor.getQueue().size() + executor.getQueue().remainingCapacity()) > 0
                ? (int) ((executor.getQueue().size() * 100.0) / (executor.getQueue().size() + executor.getQueue().remainingCapacity()))
                : 0;
            metrics.put("queueUtilizationPercent", queueUtilizationPercent);
            
            log.info("Thread pool metrics: active={}, pool={}, queue={}", 
                executor.getActiveCount(), executor.getPoolSize(), executor.getQueue().size());
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            log.error("Error fetching thread pool metrics: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch thread pool metrics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get detailed thread pool information
     * 
     * Example: GET http://localhost:8080/api/threadpool/details
     */
    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getThreadPoolDetails() {
        log.info("Fetching detailed thread pool information");
        
        try {
            ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();
            
            Map<String, Object> details = new HashMap<>();
            
            // Basic Info
            Map<String, Object> basicInfo = new HashMap<>();
            basicInfo.put("poolName", "TaskExecutor");
            basicInfo.put("className", executor.getClass().getSimpleName());
            details.put("basicInfo", basicInfo);
            
            // Configuration
            Map<String, Object> config = new HashMap<>();
            config.put("corePoolSize", executor.getCorePoolSize());
            config.put("maximumPoolSize", executor.getMaximumPoolSize());
            config.put("keepAliveTime", executor.getKeepAliveTime(TimeUnit.SECONDS) + " seconds");
            config.put("allowCoreThreadTimeOut", executor.allowsCoreThreadTimeOut());
            config.put("queueType", executor.getQueue().getClass().getSimpleName());
            config.put("queueCapacity", executor.getQueue().size() + executor.getQueue().remainingCapacity());
            details.put("configuration", config);
            
            // Current State
            Map<String, Object> state = new HashMap<>();
            state.put("activeCount", executor.getActiveCount());
            state.put("poolSize", executor.getPoolSize());
            state.put("largestPoolSize", executor.getLargestPoolSize());
            state.put("taskCount", executor.getTaskCount());
            state.put("completedTaskCount", executor.getCompletedTaskCount());
            state.put("queueSize", executor.getQueue().size());
            state.put("queueRemainingCapacity", executor.getQueue().remainingCapacity());
            details.put("currentState", state);
            
            // Status
            Map<String, Object> status = new HashMap<>();
            status.put("isShutdown", executor.isShutdown());
            status.put("isTerminated", executor.isTerminated());
            status.put("isTerminating", executor.isTerminating());
            status.put("timestamp", System.currentTimeMillis());
            details.put("status", status);
            
            return ResponseEntity.ok(details);
            
        } catch (Exception e) {
            log.error("Error fetching thread pool details: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch thread pool details");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get simple thread pool status
     * 
     * Example: GET http://localhost:8080/api/threadpool/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getThreadPoolStatus() {
        log.info("Fetching thread pool status");
        
        try {
            ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();
            
            Map<String, Object> status = new HashMap<>();
            status.put("status", "RUNNING");
            status.put("active", executor.getActiveCount());
            status.put("poolSize", executor.getPoolSize());
            status.put("queueSize", executor.getQueue().size());
            status.put("healthy", !executor.isShutdown() && !executor.isTerminated());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Error fetching thread pool status: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("healthy", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Real-time thread pool metrics stream using Server-Sent Events (SSE)
     * 
     * Example: GET http://localhost:8080/api/threadpool/stream
     * 
     * This endpoint provides real-time updates every second.
     * Open in browser to see live streaming data.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamThreadPoolMetrics() {
        log.info("New SSE connection established for thread pool metrics streaming");
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        
        emitter.onCompletion(() -> {
            log.info("SSE connection completed");
            emitters.remove(emitter);
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE connection timed out");
            emitters.remove(emitter);
        });
        
        emitter.onError((e) -> {
            log.error("SSE connection error: {}", e.getMessage());
            emitters.remove(emitter);
        });
        
        // Start streaming if not already started
        if (emitters.size() == 1) {
            startStreaming();
        }
        
        return emitter;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Thread Pool Monitoring API is running!");
    }

    /**
     * Start streaming metrics to all connected SSE clients
     */
    private void startStreaming() {
        scheduler.scheduleAtFixedRate(() -> {
            if (emitters.isEmpty()) {
                return;
            }
            
            try {
                ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();
                
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("poolName", "TaskExecutor");
                metrics.put("corePoolSize", executor.getCorePoolSize());
                metrics.put("maximumPoolSize", executor.getMaximumPoolSize());
                metrics.put("activeCount", executor.getActiveCount());
                metrics.put("poolSize", executor.getPoolSize());
                metrics.put("largestPoolSize", executor.getLargestPoolSize());
                metrics.put("taskCount", executor.getTaskCount());
                metrics.put("completedTaskCount", executor.getCompletedTaskCount());
                metrics.put("queueSize", executor.getQueue().size());
                metrics.put("queueRemainingCapacity", executor.getQueue().remainingCapacity());
                metrics.put("timestamp", System.currentTimeMillis());
                
                // Calculate utilization
                int utilizationPercent = executor.getMaximumPoolSize() > 0 
                    ? (int) ((executor.getActiveCount() * 100.0) / executor.getMaximumPoolSize())
                    : 0;
                metrics.put("utilizationPercent", utilizationPercent);
                
                int queueUtilizationPercent = (executor.getQueue().size() + executor.getQueue().remainingCapacity()) > 0
                    ? (int) ((executor.getQueue().size() * 100.0) / (executor.getQueue().size() + executor.getQueue().remainingCapacity()))
                    : 0;
                metrics.put("queueUtilizationPercent", queueUtilizationPercent);
                
                // Send to all connected clients
                for (SseEmitter emitter : emitters) {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("thread-pool-metrics")
                            .data(metrics));
                    } catch (IOException e) {
                        log.warn("Failed to send SSE event, removing emitter: {}", e.getMessage());
                        emitters.remove(emitter);
                    }
                }
                
            } catch (Exception e) {
                log.error("Error streaming thread pool metrics: {}", e.getMessage());
            }
            
        }, 0, 1, TimeUnit.SECONDS);
    }
}

