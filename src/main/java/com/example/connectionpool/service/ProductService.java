package com.example.connectionpool.service;

import com.example.connectionpool.dto.ExternalApiResponse;
import com.example.connectionpool.dto.ProductRequest;
import com.example.connectionpool.dto.ProductResponse;
import com.example.connectionpool.entity.Product;
import com.example.connectionpool.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final RestTemplate restTemplate;

    @Value("${external.api.url}")
    private String externalApiUrl;

    /**
     * Main endpoint logic: Query database, call external API, and update based on result
     */
    @Transactional
    public ProductResponse processProduct(Long productId) {
        log.info("Processing product with ID: {}", productId);

        // Step 1: Query the database
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        log.info("Found product: {}", product.getName());

        // Step 2: Call external dummy API
        ExternalApiResponse apiResponse = callExternalApi();
        log.info("External API response received: {}", apiResponse.getTitle());

        // Step 3: Update product based on API result
        product.setExternalApiResponse(apiResponse.getTitle());
        
        // Business logic: Update stock quantity based on API response
        if (apiResponse.getTitle().length() > 50) {
            product.setStockQuantity(product.getStockQuantity() + 10);
            log.info("Increased stock quantity by 10");
        } else {
            product.setStockQuantity(Math.max(0, product.getStockQuantity() - 5));
            log.info("Decreased stock quantity by 5");
        }

        // Save updated product
        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully");

        return mapToResponse(updatedProduct, "Product processed and updated successfully");
    }

    /**
     * Call external dummy API
     */
    private ExternalApiResponse callExternalApi() {
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

    /**
     * Create a new product
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating new product: {}", request.getName());

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());

        Product savedProduct = productRepository.save(product);
        log.info("Product created with ID: {}", savedProduct.getId());

        return mapToResponse(savedProduct, "Product created successfully");
    }

    /**
     * Get all products
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        log.info("Fetching all products");
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(p -> mapToResponse(p, null))
                .collect(Collectors.toList());
    }

    /**
     * Get product by ID
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToResponse(product, null);
    }

    /**
     * Delete product
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        log.info("Product deleted successfully");
    }

    /**
     * Helper method to map Product entity to ProductResponse DTO
     */
    private ProductResponse mapToResponse(Product product, String message) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .externalApiResponse(product.getExternalApiResponse())
                .lastUpdated(product.getLastUpdated())
                .message(message)
                .build();
    }
}

