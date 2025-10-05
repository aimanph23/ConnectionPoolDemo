package com.example.connectionpool.controller;

import com.example.connectionpool.dto.Customer;
import com.example.connectionpool.service.PostmanEchoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final PostmanEchoService postmanEchoService;

    /**
     * Get all customers from Postman Echo API
     * Example: GET http://localhost:8080/api/customers
     */
    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        log.info("Received request to get all customers");
        try {
            List<Customer> customers = postmanEchoService.getAllCustomers();
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            log.error("Error getting all customers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get customer by ID from Postman Echo API
     * Example: GET http://localhost:8080/api/customers/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable String id) {
        log.info("Received request to get customer with ID: {}", id);
        try {
            Customer customer = postmanEchoService.getCustomerById(id);
            if (customer != null) {
                return ResponseEntity.ok(customer);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting customer with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new customer in Postman Echo API
     * Example: POST http://localhost:8080/api/customers
     * Body: { "name": "John Doe", "email": "john@example.com", "phone": "123-456-7890", "address": "123 Main St" }
     */
    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        log.info("Received request to create customer: {}", customer.getName());
        try {
            Customer createdCustomer = postmanEchoService.addNewCustomer(customer);
            if (createdCustomer != null) {
                return ResponseEntity.status(HttpStatus.CREATED).body(createdCustomer);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } catch (Exception e) {
            log.error("Error creating customer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update customer in Postman Echo API
     * Example: PUT http://localhost:8080/api/customers/1
     * Body: { "name": "John Doe Updated", "email": "john.updated@example.com", "phone": "123-456-7890", "address": "456 Oak St" }
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCustomer(@PathVariable String id, @RequestBody Customer customer) {
        log.info("Received request to update customer with ID: {}", id);
        try {
            boolean success = postmanEchoService.updateCustomer(id, customer);
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error updating customer with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete customer from Postman Echo API
     * Example: DELETE http://localhost:8080/api/customers/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        log.info("Received request to delete customer with ID: {}", id);
        try {
            boolean success = postmanEchoService.removeCustomer(id);
            if (success) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error deleting customer with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== ASYNC VERSIONS ====================

    /**
     * Get all customers asynchronously from Postman Echo API
     * Example: GET http://localhost:8080/api/customers/async
     */
    @GetMapping("/async")
    public CompletableFuture<ResponseEntity<List<Customer>>> getAllCustomersAsync() {
        log.info("[ASYNC] Received request to get all customers");
        
        return postmanEchoService.getAllCustomersAsync()
                .thenApply(customers -> {
                    log.info("[ASYNC] Retrieved {} customers", customers.size());
                    return ResponseEntity.ok(customers);
                })
                .exceptionally(e -> {
                    log.error("[ASYNC] Error getting all customers: {}", e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    /**
     * Get customer by ID asynchronously from Postman Echo API
     * Example: GET http://localhost:8080/api/customers/async/1
     */
    @GetMapping("/async/{id}")
    public CompletableFuture<ResponseEntity<?>> getCustomerByIdAsync(@PathVariable String id) {
        log.info("[ASYNC] Received request to get customer with ID: {}", id);
        
        return postmanEchoService.getCustomerByIdAsync(id)
                .thenApply(customer -> {
                    if (customer != null) {
                        log.info("[ASYNC] Retrieved customer: {}", customer.getName());
                        return ResponseEntity.ok(customer);
                    } else {
                        log.warn("[ASYNC] Customer with ID {} not found", id);
                        return ResponseEntity.notFound().build();
                    }
                })
                .exceptionally(e -> {
                    log.error("[ASYNC] Error getting customer with ID {}: {}", id, e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    /**
     * Create new customer asynchronously in Postman Echo API
     * Example: POST http://localhost:8080/api/customers/async
     * Body: { "name": "John Doe", "email": "john@example.com", "phone": "123-456-7890", "address": "123 Main St" }
     */
    @PostMapping("/async")
    public CompletableFuture<ResponseEntity<?>> createCustomerAsync(@RequestBody Customer customer) {
        log.info("[ASYNC] Received request to create customer: {}", customer.getName());
        
        return postmanEchoService.addNewCustomerAsync(customer)
                .thenApply(createdCustomer -> {
                    if (createdCustomer != null) {
                        log.info("[ASYNC] Created customer: {}", createdCustomer.getName());
                        return ResponseEntity.status(HttpStatus.CREATED).body(createdCustomer);
                    } else {
                        log.warn("[ASYNC] Failed to create customer: {}", customer.getName());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                    }
                })
                .exceptionally(e -> {
                    log.error("[ASYNC] Error creating customer: {}", e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Customer API is running!");
    }
}
