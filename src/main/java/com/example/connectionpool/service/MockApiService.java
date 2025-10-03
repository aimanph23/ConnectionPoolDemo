package com.example.connectionpool.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockApiService {

    private final RestTemplate restTemplate;

    @Value("${external.delay.api.base-url}")
    private String delayApiBaseUrl;

    @Value("${external.delay.api.delay-seconds:2}")
    private int delaySeconds;

    /**
     * Calls Postman Echo delay API with configurable delay
     * URL: https://postman-echo.com/delay/{seconds}
     * This creates a real HTTP delay for testing connection pool behavior
     */
    public String callMockApi(Long productId) {
        String apiUrl = String.format("%s/%d", delayApiBaseUrl, delaySeconds);
        log.info("Calling Postman Echo API: {} for product ID: {}", apiUrl, productId);
        log.info("Expected delay: {} seconds", delaySeconds);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Make actual HTTP call to Postman Echo
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);
            
            long endTime = System.currentTimeMillis();
            long actualDelay = endTime - startTime;
            
            String delayValue = response != null && response.get("delay") != null 
                ? response.get("delay").toString() 
                : String.valueOf(delaySeconds);
            
            String resultMessage = String.format(
                "External API Response: Product %d processed successfully (took %dms, delay: %s seconds)", 
                productId, 
                actualDelay,
                delayValue
            );
            
            log.info("External API call completed. Response: {}", resultMessage);
            return resultMessage;
            
        } catch (Exception e) {
            log.error("Error calling external API: {}", e.getMessage());
            return String.format("External API Error: %s (Product %d)", e.getMessage(), productId);
        }
    }

    /**
     * Calls API with custom delay (overrides configuration)
     */
    public String callMockApiWithCustomDelay(Long productId, int customDelaySeconds) {
        String apiUrl = String.format("%s/%d", delayApiBaseUrl, customDelaySeconds);
        log.info("Calling Postman Echo API with custom delay: {} seconds for product ID: {}", 
            customDelaySeconds, productId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);
            
            long endTime = System.currentTimeMillis();
            long actualDelay = endTime - startTime;
            
            String delayValue = response != null && response.get("delay") != null 
                ? response.get("delay").toString() 
                : String.valueOf(customDelaySeconds);
            
            log.info("Custom delay API call completed. Delay: {} seconds, Actual: {}ms", delayValue, actualDelay);
            
            return String.format("External API Response: Product %d with custom delay (took %dms)", 
                               productId, actualDelay);
        } catch (Exception e) {
            log.error("Error calling external API with custom delay: {}", e.getMessage());
            return String.format("External API Error: %s", e.getMessage());
        }
    }

    /**
     * Get the configured delay for testing/monitoring purposes
     */
    public int getConfiguredDelaySeconds() {
        return delaySeconds;
    }

    /**
     * Get the configured API URL for testing/monitoring purposes
     */
    public String getApiUrl() {
        return delayApiBaseUrl;
    }

    /**
     * Get the full URL with configured delay
     */
    public String getFullApiUrl() {
        return String.format("%s/%d", delayApiBaseUrl, delaySeconds);
    }
}

