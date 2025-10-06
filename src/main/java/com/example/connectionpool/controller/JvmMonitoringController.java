package com.example.connectionpool.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.management.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/jvm")
@Slf4j
public class JvmMonitoringController {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();

    /**
     * Get comprehensive JVM metrics
     * Example: GET http://localhost:8080/api/jvm/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getJvmMetrics() {
        log.info("Fetching JVM metrics");
        
        Map<String, Object> metrics = new HashMap<>();
        
        // Memory metrics
        metrics.put("memory", getMemoryMetrics());
        
        // CPU metrics
        metrics.put("cpu", getCpuMetrics());
        
        // Thread metrics
        metrics.put("threads", getThreadMetrics());
        
        // Runtime metrics
        metrics.put("runtime", getRuntimeMetrics());
        
        // Class loading metrics
        metrics.put("classLoading", getClassLoadingMetrics());
        
        // Garbage collection metrics
        metrics.put("gc", getGcMetrics());
        
        metrics.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get memory metrics only
     * Example: GET http://localhost:8080/api/jvm/memory
     */
    @GetMapping("/memory")
    public ResponseEntity<Map<String, Object>> getMemoryMetrics() {
        Map<String, Object> memory = new HashMap<>();
        
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
        
        // Heap memory
        Map<String, Object> heap = new HashMap<>();
        heap.put("init", formatBytes(heapMemory.getInit()));
        heap.put("used", formatBytes(heapMemory.getUsed()));
        heap.put("committed", formatBytes(heapMemory.getCommitted()));
        heap.put("max", formatBytes(heapMemory.getMax()));
        heap.put("usedBytes", heapMemory.getUsed());
        heap.put("maxBytes", heapMemory.getMax());
        heap.put("usagePercent", calculatePercentage(heapMemory.getUsed(), heapMemory.getMax()));
        memory.put("heap", heap);
        
        // Non-heap memory
        Map<String, Object> nonHeap = new HashMap<>();
        nonHeap.put("init", formatBytes(nonHeapMemory.getInit()));
        nonHeap.put("used", formatBytes(nonHeapMemory.getUsed()));
        nonHeap.put("committed", formatBytes(nonHeapMemory.getCommitted()));
        nonHeap.put("max", formatBytes(nonHeapMemory.getMax()));
        nonHeap.put("usedBytes", nonHeapMemory.getUsed());
        nonHeap.put("maxBytes", nonHeapMemory.getMax());
        memory.put("nonHeap", nonHeap);
        
        // Total memory
        long totalUsed = heapMemory.getUsed() + nonHeapMemory.getUsed();
        long totalMax = heapMemory.getMax() + (nonHeapMemory.getMax() > 0 ? nonHeapMemory.getMax() : 0);
        memory.put("totalUsed", formatBytes(totalUsed));
        memory.put("totalMax", formatBytes(totalMax));
        
        return ResponseEntity.ok(memory);
    }

    /**
     * Get CPU metrics
     * Example: GET http://localhost:8080/api/jvm/cpu
     */
    @GetMapping("/cpu")
    public ResponseEntity<Map<String, Object>> getCpuMetrics() {
        Map<String, Object> cpu = new HashMap<>();
        
        cpu.put("availableProcessors", osMXBean.getAvailableProcessors());
        cpu.put("systemLoadAverage", osMXBean.getSystemLoadAverage());
        
        // Try to get process CPU load if available
        if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsMXBean = 
                (com.sun.management.OperatingSystemMXBean) osMXBean;
            cpu.put("processCpuLoad", String.format("%.2f%%", sunOsMXBean.getProcessCpuLoad() * 100));
            cpu.put("systemCpuLoad", String.format("%.2f%%", sunOsMXBean.getSystemCpuLoad() * 100));
            cpu.put("processCpuTime", formatDuration(sunOsMXBean.getProcessCpuTime()));
        }
        
        return ResponseEntity.ok(cpu);
    }

    /**
     * Get thread metrics
     * Example: GET http://localhost:8080/api/jvm/threads
     */
    @GetMapping("/threads")
    public ResponseEntity<Map<String, Object>> getThreadMetrics() {
        Map<String, Object> threads = new HashMap<>();
        
        threads.put("threadCount", threadMXBean.getThreadCount());
        threads.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        threads.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
        threads.put("totalStartedThreadCount", threadMXBean.getTotalStartedThreadCount());
        
        // Thread states
        long[] threadIds = threadMXBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds);
        
        Map<Thread.State, Integer> stateCount = new HashMap<>();
        for (ThreadInfo info : threadInfos) {
            if (info != null) {
                Thread.State state = info.getThreadState();
                stateCount.put(state, stateCount.getOrDefault(state, 0) + 1);
            }
        }
        threads.put("threadStates", stateCount);
        
        return ResponseEntity.ok(threads);
    }

    /**
     * Get runtime metrics
     * Example: GET http://localhost:8080/api/jvm/runtime
     */
    @GetMapping("/runtime")
    public ResponseEntity<Map<String, Object>> getRuntimeMetrics() {
        Map<String, Object> runtime = new HashMap<>();
        
        runtime.put("vmName", runtimeMXBean.getVmName());
        runtime.put("vmVendor", runtimeMXBean.getVmVendor());
        runtime.put("vmVersion", runtimeMXBean.getVmVersion());
        runtime.put("uptime", formatDuration(runtimeMXBean.getUptime()));
        runtime.put("uptimeMillis", runtimeMXBean.getUptime());
        runtime.put("startTime", runtimeMXBean.getStartTime());
        runtime.put("inputArguments", runtimeMXBean.getInputArguments());
        
        return ResponseEntity.ok(runtime);
    }

    /**
     * Get class loading metrics
     * Example: GET http://localhost:8080/api/jvm/classes
     */
    @GetMapping("/classes")
    public ResponseEntity<Map<String, Object>> getClassLoadingMetrics() {
        Map<String, Object> classes = new HashMap<>();
        
        classes.put("loadedClassCount", classLoadingMXBean.getLoadedClassCount());
        classes.put("totalLoadedClassCount", classLoadingMXBean.getTotalLoadedClassCount());
        classes.put("unloadedClassCount", classLoadingMXBean.getUnloadedClassCount());
        
        return ResponseEntity.ok(classes);
    }

    /**
     * Get garbage collection metrics
     * Example: GET http://localhost:8080/api/jvm/gc
     */
    @GetMapping("/gc")
    public ResponseEntity<Map<String, Object>> getGcMetrics() {
        Map<String, Object> gc = new HashMap<>();
        
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> gcInfo = new HashMap<>();
            gcInfo.put("collectionCount", gcBean.getCollectionCount());
            gcInfo.put("collectionTime", formatDuration(gcBean.getCollectionTime()));
            gcInfo.put("collectionTimeMillis", gcBean.getCollectionTime());
            gc.put(gcBean.getName(), gcInfo);
        }
        
        return ResponseEntity.ok(gc);
    }

    /**
     * Real-time JVM metrics stream using Server-Sent Events (SSE)
     * Example: GET http://localhost:8080/api/jvm/stream
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJvmMetrics() {
        log.info("New SSE connection established for JVM metrics");
        
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
        return ResponseEntity.ok("JVM Monitoring API is running!");
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
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("memory", getMemoryMetrics().getBody());
                metrics.put("cpu", getCpuMetrics().getBody());
                metrics.put("threads", getThreadMetrics().getBody());
                metrics.put("timestamp", System.currentTimeMillis());
                
                // Send to all connected clients
                for (SseEmitter emitter : emitters) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("jvm-metrics")
                                .data(metrics));
                    } catch (Exception e) {
                        log.error("Error sending SSE event: {}", e.getMessage());
                        emitters.remove(emitter);
                    }
                }
            } catch (Exception e) {
                log.error("Error collecting JVM metrics: {}", e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    // Helper methods
    
    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        if (hours > 0) return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds % 60);
        return String.format("%ds", seconds);
    }
    
    private int calculatePercentage(long used, long max) {
        if (max <= 0) return 0;
        return (int) ((used * 100.0) / max);
    }
}
