package com.example.connectionpool.repository;

import com.example.connectionpool.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStockQuantityGreaterThan(Integer quantity);

    @Query("SELECT p FROM Product p WHERE p.price < :maxPrice ORDER BY p.price DESC")
    List<Product> findProductsUnderPrice(Double maxPrice);
}

