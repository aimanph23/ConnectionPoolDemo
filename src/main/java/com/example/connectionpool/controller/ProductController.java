package com.example.connectionpool.controller;

import com.example.connectionpool.dto.ProductRequest;
import com.example.connectionpool.dto.ProductResponse;
import com.example.connectionpool.service.ProductService;
import com.example.connectionpool.service.ProductServiceAsync;
import com.example.connectionpool.service.MockApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final ProductServiceAsync productServiceAsync;
    private final MockApiService mockApiService;
    
    @Value("${product.api.sleep.ms:0}")
    private long productApiSleepMs;

    /**
     * Main endpoint: Process a product - queries DB, calls external API, and updates based on result
     * 
     * Example: POST http://localhost:8080/api/products/1/process
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<ProductResponse> processProduct(@PathVariable Long id) {
        log.info("Received request to process product with ID: {}", id);
        try {
            ProductResponse response = productService.processProduct(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error processing product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ProductResponse.builder()
                    .message("Error: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Create a new product
     * 
     * Example: POST http://localhost:8080/api/products
     * Body: { "name": "Laptop", "description": "High-end laptop", "price": 1200.00, "stockQuantity": 50 }
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        log.info("Received request to create product: {}", request.getName());
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all products
     * 
     * Example: GET http://localhost:8080/api/products
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("Received request to get all products");
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Get product by ID
     * Includes a mock API call with configurable delay
     * 
     * Example: GET http://localhost:8080/api/products/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        log.info("Received request to get product with ID: {}", id);
        try {
            ProductResponse response = productService.getProductById(id);
            
            // Call mock API with configurable delay
            String mockApiResponse = mockApiService.callMockApi(id);
            log.info("Mock API response: {}", mockApiResponse);
            
            // Configurable sleep after API call
            if (productApiSleepMs > 0) {
                log.info("Sleeping for {} ms after API call", productApiSleepMs);
                Thread.sleep(productApiSleepMs);
            }
            
            // Optionally add mock response to the product response message
            //if (response.getMessage() == null || response.getMessage().isEmpty()) {
            //    response.setMessage(mockApiResponse);
            //} else {
            //    response.setMessage(response.getMessage() + " | " + mockApiResponse);
            //}

            ProductResponse response2 = productService.getProductById(id+1);

            return ResponseEntity.ok(response2);
        } catch (InterruptedException e) {
            log.error("Thread interrupted during sleep: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ProductResponse.builder()
                    .message("Error: Thread interrupted")
                    .build()
            );
        } catch (RuntimeException e) {
            log.error("Error getting product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ProductResponse.builder()
                    .message("Error: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Delete product
     * 
     * Example: DELETE http://localhost:8080/api/products/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Received request to delete product with ID: {}", id);
        try {
            productService.deleteProduct(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get product by ID - V2 NON-BLOCKING VERSION
     * Uses async processing to release DB connection before calling mock API
     * This prevents connection pool exhaustion during long-running external API calls
     * 
     * Example: GET http://localhost:8080/api/products/v2/1
     */
    @GetMapping("/v2/{id}")
    public CompletableFuture<ResponseEntity<ProductResponse>> getProductByIdV2(@PathVariable Long id) {
        log.info("[V2] Received non-blocking request to get product with ID: {}", id);
        
        return productServiceAsync.getProductWithMockApiAsync(id)
                .thenApply(response -> {
                    log.info("[V2] Non-blocking request completed for product ID: {}", id);

                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("[V2] Error in non-blocking request: {}", e.getMessage());
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ProductResponse.builder()
                            .message("Error: " + e.getMessage())
                            .build()
                    );
                });
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Product API is running!");
    }
}

