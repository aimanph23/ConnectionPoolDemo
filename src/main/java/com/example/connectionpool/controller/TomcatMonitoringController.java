package com.example.connectionpool.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/tomcat")
@Slf4j
public class TomcatMonitoringController {

    private final ServletWebServerApplicationContext applicationContext;
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public TomcatMonitoringController(ServletWebServerApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Get Tomcat thread pool metrics
     * 
     * Example: GET http://localhost:8080/api/tomcat/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getTomcatMetrics() {
        log.info("Fetching Tomcat thread pool metrics");
        
        try {
            Map<String, Object> metrics = extractTomcatMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error fetching Tomcat metrics: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch Tomcat metrics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get detailed Tomcat information
     * 
     * Example: GET http://localhost:8080/api/tomcat/details
     */
    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getTomcatDetails() {
        log.info("Fetching detailed Tomcat information");
        
        try {
            Map<String, Object> details = new HashMap<>();
            Map<String, Object> metrics = extractTomcatMetrics();
            
            // Basic Info
            Map<String, Object> basicInfo = new HashMap<>();
            basicInfo.put("serverInfo", "Apache Tomcat (Embedded)");
            basicInfo.put("containerType", "Tomcat");
            details.put("basicInfo", basicInfo);
            
            // Configuration
            Map<String, Object> config = new HashMap<>();
            config.put("maxThreads", metrics.getOrDefault("maxThreads", "N/A"));
            config.put("minSpareThreads", metrics.getOrDefault("minSpareThreads", "N/A"));
            config.put("maxConnections", metrics.getOrDefault("maxConnections", "N/A"));
            config.put("acceptCount", metrics.getOrDefault("acceptCount", "N/A"));
            details.put("configuration", config);
            
            // Current State
            Map<String, Object> state = new HashMap<>();
            state.put("currentThreadCount", metrics.getOrDefault("currentThreadCount", 0));
            state.put("currentThreadsBusy", metrics.getOrDefault("currentThreadsBusy", 0));
            state.put("connectionCount", metrics.getOrDefault("connectionCount", 0));
            details.put("currentState", state);
            
            // Status
            Map<String, Object> status = new HashMap<>();
            status.put("status", "RUNNING");
            status.put("timestamp", System.currentTimeMillis());
            details.put("status", status);
            
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            log.error("Error fetching Tomcat details: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch Tomcat details");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get simple Tomcat status
     * 
     * Example: GET http://localhost:8080/api/tomcat/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTomcatStatus() {
        log.info("Fetching Tomcat status");
        
        try {
            Map<String, Object> metrics = extractTomcatMetrics();
            
            Map<String, Object> status = new HashMap<>();
            status.put("status", "RUNNING");
            status.put("currentThreads", metrics.getOrDefault("currentThreadCount", 0));
            status.put("busyThreads", metrics.getOrDefault("currentThreadsBusy", 0));
            status.put("connections", metrics.getOrDefault("connectionCount", 0));
            status.put("healthy", true);
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error fetching Tomcat status: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("healthy", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Real-time Tomcat metrics stream using Server-Sent Events (SSE)
     * 
     * Example: GET http://localhost:8080/api/tomcat/stream
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTomcatMetrics() {
        log.info("New SSE connection established for Tomcat metrics streaming");
        
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
        return ResponseEntity.ok("Tomcat Monitoring API is running!");
    }

    /**
     * Extract Tomcat metrics using reflection
     */
    private Map<String, Object> extractTomcatMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            WebServer webServer = applicationContext.getWebServer();
            
            if (webServer instanceof TomcatWebServer) {
                TomcatWebServer tomcatWebServer = (TomcatWebServer) webServer;
                
                // Use reflection to access Tomcat internals
                Field tomcatField = TomcatWebServer.class.getDeclaredField("tomcat");
                tomcatField.setAccessible(true);
                org.apache.catalina.startup.Tomcat tomcat = 
                    (org.apache.catalina.startup.Tomcat) tomcatField.get(tomcatWebServer);
                
                Server server = tomcat.getServer();
                Service[] services = server.findServices();
                
                if (services.length > 0) {
                    Service service = services[0];
                    Connector[] connectors = service.findConnectors();
                    
                    if (connectors.length > 0) {
                        Connector connector = connectors[0];
                        org.apache.coyote.ProtocolHandler protocolHandler = connector.getProtocolHandler();
                        
                        // Get executor if available
                        if (protocolHandler.getExecutor() instanceof ThreadPoolExecutor) {
                            ThreadPoolExecutor executor = (ThreadPoolExecutor) protocolHandler.getExecutor();
                            
                            metrics.put("currentThreadCount", executor.getPoolSize());
                            metrics.put("currentThreadsBusy", executor.getActiveCount());
                            metrics.put("maxThreads", executor.getMaximumPoolSize());
                            metrics.put("minSpareThreads", executor.getCorePoolSize());
                            metrics.put("completedTaskCount", executor.getCompletedTaskCount());
                            metrics.put("taskCount", executor.getTaskCount());
                            metrics.put("queueSize", executor.getQueue().size());
                            metrics.put("largestPoolSize", executor.getLargestPoolSize());
                            
                            // Calculate utilization
                            int utilization = executor.getMaximumPoolSize() > 0
                                ? (int) ((executor.getActiveCount() * 100.0) / executor.getMaximumPoolSize())
                                : 0;
                            metrics.put("utilizationPercent", utilization);
                        }
                        
                        // Get connection info (use safe methods)
                        try {
                            Object maxConn = connector.getProperty("maxConnections");
                            metrics.put("maxConnections", maxConn != null ? maxConn : "N/A");
                        } catch (Exception e) {
                            metrics.put("maxConnections", "N/A");
                        }
                        
                        try {
                            Object acceptCnt = connector.getProperty("acceptCount");
                            metrics.put("acceptCount", acceptCnt != null ? acceptCnt : "N/A");
                        } catch (Exception e) {
                            metrics.put("acceptCount", "N/A");
                        }
                        
                        try {
                            // Try to get connection count via reflection if available
                            java.lang.reflect.Method method = protocolHandler.getClass().getMethod("getCurrentConnectionCount");
                            metrics.put("connectionCount", method.invoke(protocolHandler));
                        } catch (Exception e) {
                            // Method not available in this Tomcat version
                            metrics.put("connectionCount", 0);
                        }
                    }
                }
            }
            
            metrics.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("Error extracting Tomcat metrics: {}", e.getMessage(), e);
            metrics.put("error", "Unable to extract metrics: " + e.getMessage());
            metrics.put("currentThreadCount", 0);
            metrics.put("currentThreadsBusy", 0);
            metrics.put("utilizationPercent", 0);
        }
        
        return metrics;
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
                Map<String, Object> metrics = extractTomcatMetrics();
                
                // Send to all connected clients
                for (SseEmitter emitter : emitters) {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("tomcat-metrics")
                            .data(metrics));
                    } catch (IOException e) {
                        log.warn("Failed to send SSE event, removing emitter: {}", e.getMessage());
                        emitters.remove(emitter);
                    }
                }
                
            } catch (Exception e) {
                log.error("Error streaming Tomcat metrics: {}", e.getMessage());
            }
            
        }, 0, 1, TimeUnit.SECONDS);
    }
}

