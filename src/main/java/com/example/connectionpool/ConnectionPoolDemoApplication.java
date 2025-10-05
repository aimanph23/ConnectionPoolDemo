package com.example.connectionpool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableAsync
public class ConnectionPoolDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectionPoolDemoApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * TaskExecutor bean for @Async methods
     * 
     * Java 21 Virtual Threads Option:
     * With Java 21, you can use virtual threads for async operations!
     * This bean now uses virtual threads for ultimate scalability.
     * 
     * To switch back to platform threads, uncomment the ThreadPoolTaskExecutor code below.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        // Option 1: Use Java 21 Virtual Threads (Recommended!)
        // Creates a new virtual thread for each task
        // Unlimited concurrency, minimal memory overhead
        return Executors.newVirtualThreadPerTaskExecutor();
        
        /* Option 2: Traditional Platform Threads (Old Way)
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
        */
    }
}

