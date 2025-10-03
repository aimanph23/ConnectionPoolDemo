package com.example.connectionpool.service;

import com.example.connectionpool.dto.ProductResponse;
import com.example.connectionpool.entity.Product;
import com.example.connectionpool.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceAsync {

    private final ProductRepository productRepository;
    private final MockApiService mockApiService;

    /**
     * Non-blocking version: Fetches product from DB and releases connection immediately
     * This prevents holding a DB connection during the mock API call
     */
    @Async("taskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Product> getProductByIdAsync(Long id) {
        log.info("[ASYNC] Fetching product {} from database - Thread: {}", 
            id, Thread.currentThread().getName());
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        log.info("[ASYNC] Product {} fetched successfully, DB connection will be released - Thread: {}", 
            id, Thread.currentThread().getName());
        
        // Return immediately - transaction ends here, DB connection is released
        return CompletableFuture.completedFuture(product);
    }

    /**
     * Non-blocking mock API call
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
     * Combines product data with mock API response asynchronously
     * DB connection is only held during the brief query, not during the mock API delay
     */
    public CompletableFuture<ProductResponse> getProductWithMockApiAsync(Long id) {
        long startTime = System.currentTimeMillis();
        
        log.info("[ASYNC] Starting non-blocking product fetch for ID: {}", id);
        
        // Step 1: Fetch product from DB (async, releases connection immediately after query)
        CompletableFuture<Product> productFuture = getProductByIdAsync(id);
        
        // Step 2: Call mock API (async, runs in parallel or after product fetch)
        // Important: This happens AFTER the DB transaction completes and connection is released
        CompletableFuture<String> mockApiFuture = productFuture.thenCompose(product -> {
            log.info("[ASYNC] Product fetched, now calling mock API without holding DB connection");
            return callMockApiAsync(id);
        });
        
        // Step 3: Combine results
        return productFuture.thenCombine(mockApiFuture, (product, mockApiResponse) -> {
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            log.info("[ASYNC] Combining results for product {} - Total time: {}ms", id, totalTime);
            
            return ProductResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .stockQuantity(product.getStockQuantity())
                    .externalApiResponse(product.getExternalApiResponse())
                    .lastUpdated(product.getLastUpdated())
                    .message(String.format("%s | Total processing time: %dms", mockApiResponse, totalTime))
                    .build();
        });
    }
}

