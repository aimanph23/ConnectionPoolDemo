package com.example.connectionpool.service;

import com.example.connectionpool.dto.ExternalApiResponse;
import com.example.connectionpool.dto.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostmanEchoService {

    private final MockApiService mockApiService;
    private final RestTemplate restTemplate;

    @Value("${external.api.url}")
    private String externalApiUrl;

    @Value("${postman.api.base-url:https://postman-echo.com}")
    private String postmanApiBaseUrl;

    /**
     * Non-blocking mock API call to Postman Echo
     */
    @Async("taskExecutor")
    public CompletableFuture<String> callMockApiAsync(Long productId) {
        log.info("[ASYNC] Calling mock API for product {} - Thread: {}, NO DB connection held", 
            productId, Thread.currentThread().getName());
        
        String mockResponse = mockApiService.callMockApi(productId);
        
        log.info("[ASYNC] Mock API call completed for product {} - Thread: {}", 
            productId, Thread.currentThread().getName());
        
        return CompletableFuture.completedFuture(mockResponse);
    }

    /**
     * Call external dummy API
     */
    public ExternalApiResponse callExternalApi() {
        try {
            log.info("Calling external API: {}", externalApiUrl);
            ExternalApiResponse response = restTemplate.getForObject(externalApiUrl, ExternalApiResponse.class);
            return response != null ? response : new ExternalApiResponse();
        } catch (Exception e) {
            log.error("Error calling external API: {}", e.getMessage());
            // Return default response on error
            ExternalApiResponse defaultResponse = new ExternalApiResponse();
            defaultResponse.setTitle("Default Response - API call failed");
            return defaultResponse;
        }
    }

    // ==================== POSTMAN API 101 COLLECTION METHODS ====================

    /**
     * Retrieve all customers from Postman Echo API
     * Endpoint: GET /customers
     */
    public List<Customer> getAllCustomers() {
        try {
            String url = postmanApiBaseUrl + "/customers";
            log.info("Retrieving all customers from: {}", url);
            
            ResponseEntity<Customer[]> response = restTemplate.getForEntity(url, Customer[].class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Customer> customers = Arrays.asList(response.getBody());
                log.info("Successfully retrieved {} customers", customers.size());
                return customers;
            } else {
                log.warn("No customers found or empty response");
                return Arrays.asList();
            }
        } catch (Exception e) {
            log.error("Error retrieving all customers: {}", e.getMessage());
            return Arrays.asList();
        }
    }

    /**
     * Get one customer by ID from Postman Echo API
     * Endpoint: GET /customers/{id}
     */
    public Customer getCustomerById(String id) {
        try {
            String url = postmanApiBaseUrl + "/customers/" + id;
            log.info("Retrieving customer with ID: {} from: {}", id, url);
            
            Customer customer = restTemplate.getForObject(url, Customer.class);
            
            if (customer != null) {
                log.info("Successfully retrieved customer: {}", customer.getName());
            } else {
                log.warn("Customer with ID {} not found", id);
            }
            
            return customer;
        } catch (Exception e) {
            log.error("Error retrieving customer with ID {}: {}", id, e.getMessage());
            return null;
        }
    }

    /**
     * Add new customer to Postman Echo API
     * Endpoint: POST /customers
     */
    public Customer addNewCustomer(Customer customer) {
        try {
            String url = postmanApiBaseUrl + "/customers";
            log.info("Adding new customer: {} to: {}", customer.getName(), url);
            
            Customer createdCustomer = restTemplate.postForObject(url, customer, Customer.class);
            
            if (createdCustomer != null) {
                log.info("Successfully added customer: {}", createdCustomer.getName());
            } else {
                log.warn("Failed to add customer: {}", customer.getName());
            }
            
            return createdCustomer;
        } catch (Exception e) {
            log.error("Error adding new customer: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Update customer in Postman Echo API
     * Endpoint: PUT /customers/{id}
     */
    public boolean updateCustomer(String id, Customer customer) {
        try {
            String url = postmanApiBaseUrl + "/customers/" + id;
            log.info("Updating customer with ID: {} at: {}", id, url);
            
            restTemplate.put(url, customer);
            log.info("Successfully updated customer with ID: {}", id);
            return true;
        } catch (Exception e) {
            log.error("Error updating customer with ID {}: {}", id, e.getMessage());
            return false;
        }
    }

    /**
     * Remove customer from Postman Echo API
     * Endpoint: DELETE /customers/{id}
     */
    public boolean removeCustomer(String id) {
        try {
            String url = postmanApiBaseUrl + "/customers/" + id;
            log.info("Removing customer with ID: {} from: {}", id, url);
            
            restTemplate.delete(url);
            log.info("Successfully removed customer with ID: {}", id);
            return true;
        } catch (Exception e) {
            log.error("Error removing customer with ID {}: {}", id, e.getMessage());
            return false;
        }
    }

    /**
     * Async version: Retrieve all customers from Postman Echo API
     */
    @Async("taskExecutor")
    public CompletableFuture<List<Customer>> getAllCustomersAsync() {
        log.info("[ASYNC] Retrieving all customers - Thread: {}", Thread.currentThread().getName());
        
        List<Customer> customers = getAllCustomers();
        
        log.info("[ASYNC] Retrieved {} customers - Thread: {}", 
            customers.size(), Thread.currentThread().getName());
        
        return CompletableFuture.completedFuture(customers);
    }

    /**
     * Async version: Get one customer by ID from Postman Echo API
     */
    @Async("taskExecutor")
    public CompletableFuture<Customer> getCustomerByIdAsync(String id) {
        log.info("[ASYNC] Retrieving customer with ID: {} - Thread: {}", 
            id, Thread.currentThread().getName());
        
        Customer customer = getCustomerById(id);
        
        log.info("[ASYNC] Retrieved customer: {} - Thread: {}", 
            customer != null ? customer.getName() : "null", Thread.currentThread().getName());
        
        return CompletableFuture.completedFuture(customer);
    }

    /**
     * Async version: Add new customer to Postman Echo API
     */
    @Async("taskExecutor")
    public CompletableFuture<Customer> addNewCustomerAsync(Customer customer) {
        log.info("[ASYNC] Adding new customer: {} - Thread: {}", 
            customer.getName(), Thread.currentThread().getName());
        
        Customer createdCustomer = addNewCustomer(customer);
        
        log.info("[ASYNC] Added customer: {} - Thread: {}", 
            createdCustomer != null ? createdCustomer.getName() : "null", Thread.currentThread().getName());
        
        return CompletableFuture.completedFuture(createdCustomer);
    }
}
