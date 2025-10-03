package com.example.connectionpool.controller;

import com.example.connectionpool.dto.ConnectionPoolInfo;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Slf4j
public class MonitoringController {

    private final DataSource dataSource;
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Get HikariCP connection pool metrics
     * 
     * Example: GET http://localhost:8080/api/monitoring/hikari
     */
    @GetMapping("/hikari")
    public ResponseEntity<ConnectionPoolInfo> getHikariMetrics() {
        log.info("Fetching HikariCP connection pool metrics");
        
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                
                ConnectionPoolInfo info = ConnectionPoolInfo.builder()
                    .poolName(hikariDataSource.getPoolName())
                    .totalConnections(poolMXBean.getTotalConnections())
                    .activeConnections(poolMXBean.getActiveConnections())
                    .idleConnections(poolMXBean.getIdleConnections())
                    .threadsAwaitingConnection(poolMXBean.getThreadsAwaitingConnection())
                    .maximumPoolSize(hikariDataSource.getMaximumPoolSize())
                    .minimumIdle(hikariDataSource.getMinimumIdle())
                    .connectionTimeout(hikariDataSource.getConnectionTimeout())
                    .idleTimeout(hikariDataSource.getIdleTimeout())
                    .maxLifetime(hikariDataSource.getMaxLifetime())
                    .status("healthy")
                    .timestamp(System.currentTimeMillis())
                    .build();
                
                log.info("Connection pool metrics - Active: {}, Idle: {}, Total: {}, Waiting: {}", 
                    info.getActiveConnections(), 
                    info.getIdleConnections(), 
                    info.getTotalConnections(),
                    info.getThreadsAwaitingConnection());
                
                return ResponseEntity.ok(info);
            } else {
                log.warn("DataSource is not a HikariDataSource instance");
                ConnectionPoolInfo info = ConnectionPoolInfo.builder()
                    .status("error")
                    .poolName("unknown")
                    .timestamp(System.currentTimeMillis())
                    .build();
                return ResponseEntity.ok(info);
            }
        } catch (Exception e) {
            log.error("Error fetching HikariCP metrics: {}", e.getMessage());
            ConnectionPoolInfo info = ConnectionPoolInfo.builder()
                .status("error: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
            return ResponseEntity.status(500).body(info);
        }
    }

    /**
     * Get detailed connection pool statistics as a map
     * 
     * Example: GET http://localhost:8080/api/monitoring/hikari/details
     */
    @GetMapping("/hikari/details")
    public ResponseEntity<Map<String, Object>> getHikariDetailsMap() {
        log.info("Fetching detailed HikariCP connection pool metrics");
        
        Map<String, Object> details = new HashMap<>();
        
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                
                // Connection Pool Status
                details.put("poolName", hikariDataSource.getPoolName());
                details.put("status", "healthy");
                
                // Current State
                Map<String, Object> currentState = new HashMap<>();
                currentState.put("totalConnections", poolMXBean.getTotalConnections());
                currentState.put("activeConnections", poolMXBean.getActiveConnections());
                currentState.put("idleConnections", poolMXBean.getIdleConnections());
                currentState.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
                details.put("currentState", currentState);
                
                // Configuration
                Map<String, Object> config = new HashMap<>();
                config.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
                config.put("minimumIdle", hikariDataSource.getMinimumIdle());
                config.put("connectionTimeout", hikariDataSource.getConnectionTimeout() + "ms");
                config.put("idleTimeout", hikariDataSource.getIdleTimeout() + "ms");
                config.put("maxLifetime", hikariDataSource.getMaxLifetime() + "ms");
                config.put("jdbcUrl", hikariDataSource.getJdbcUrl());
                config.put("driverClassName", hikariDataSource.getDriverClassName());
                details.put("configuration", config);
                
                // Health Indicators
                Map<String, Object> health = new HashMap<>();
                health.put("poolUtilization", String.format("%.2f%%", 
                    (poolMXBean.getActiveConnections() * 100.0) / hikariDataSource.getMaximumPoolSize()));
                health.put("hasWaitingThreads", poolMXBean.getThreadsAwaitingConnection() > 0);
                health.put("isPoolFull", poolMXBean.getTotalConnections() >= hikariDataSource.getMaximumPoolSize());
                health.put("hasIdleConnections", poolMXBean.getIdleConnections() > 0);
                details.put("health", health);
                
                details.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(details);
            } else {
                details.put("status", "error");
                details.put("message", "DataSource is not a HikariDataSource instance");
                return ResponseEntity.ok(details);
            }
        } catch (Exception e) {
            log.error("Error fetching detailed HikariCP metrics: {}", e.getMessage());
            details.put("status", "error");
            details.put("message", e.getMessage());
            return ResponseEntity.status(500).body(details);
        }
    }

    /**
     * Get simple connection pool status (quick check)
     * 
     * Example: GET http://localhost:8080/api/monitoring/hikari/status
     */
    @GetMapping("/hikari/status")
    public ResponseEntity<Map<String, Object>> getQuickStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                
                status.put("active", poolMXBean.getActiveConnections());
                status.put("idle", poolMXBean.getIdleConnections());
                status.put("total", poolMXBean.getTotalConnections());
                status.put("waiting", poolMXBean.getThreadsAwaitingConnection());
                status.put("max", hikariDataSource.getMaximumPoolSize());
                status.put("healthy", poolMXBean.getThreadsAwaitingConnection() == 0);
                
                return ResponseEntity.ok(status);
            } else {
                status.put("error", "Not a HikariDataSource");
                return ResponseEntity.ok(status);
            }
        } catch (Exception e) {
            status.put("error", e.getMessage());
            return ResponseEntity.status(500).body(status);
        }
    }

    /**
     * Server-Sent Events (SSE) endpoint for real-time metrics streaming
     * Sends connection pool metrics every 500ms
     * 
     * Example: GET http://localhost:8080/api/monitoring/hikari/stream
     */
    @GetMapping(value = "/hikari/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamHikariMetrics() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        
        log.info("New SSE connection established. Total connections: {}", emitters.size());
        
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("SSE connection completed. Remaining connections: {}", emitters.size());
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.info("SSE connection timed out. Remaining connections: {}", emitters.size());
        });
        
        emitter.onError((e) -> {
            emitters.remove(emitter);
            log.error("SSE connection error: {}", e.getMessage());
        });
        
        // Start streaming if this is the first connection
        if (emitters.size() == 1) {
            startMetricsStreaming();
        }
        
        return emitter;
    }

    /**
     * Start streaming metrics to all connected clients
     */
    private void startMetricsStreaming() {
        scheduler.scheduleAtFixedRate(() -> {
            if (emitters.isEmpty()) {
                return;
            }
            
            try {
                Map<String, Object> metrics = getCurrentMetrics();
                
                for (SseEmitter emitter : emitters) {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("metrics")
                            .data(metrics));
                    } catch (IOException e) {
                        emitters.remove(emitter);
                        log.warn("Failed to send metrics to client, removing emitter");
                    }
                }
            } catch (Exception e) {
                log.error("Error streaming metrics: {}", e.getMessage());
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * Get current metrics as a map
     */
    private Map<String, Object> getCurrentMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                
                metrics.put("timestamp", System.currentTimeMillis());
                metrics.put("active", poolMXBean.getActiveConnections());
                metrics.put("idle", poolMXBean.getIdleConnections());
                metrics.put("total", poolMXBean.getTotalConnections());
                metrics.put("waiting", poolMXBean.getThreadsAwaitingConnection());
                metrics.put("max", hikariDataSource.getMaximumPoolSize());
                
                // Calculate utilization percentage
                int active = poolMXBean.getActiveConnections();
                int max = hikariDataSource.getMaximumPoolSize();
                double utilization = max > 0 ? (active * 100.0) / max : 0;
                metrics.put("utilization", Math.round(utilization * 10) / 10.0);
                
                metrics.put("healthy", poolMXBean.getThreadsAwaitingConnection() == 0);
            }
        } catch (Exception e) {
            log.error("Error getting current metrics: {}", e.getMessage());
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }

    /**
     * Health check endpoint
     * 
     * Example: GET http://localhost:8080/api/monitoring/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Monitoring API is running!");
    }
}

