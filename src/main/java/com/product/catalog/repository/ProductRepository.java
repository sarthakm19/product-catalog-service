package com.product.catalog.repository;

import com.product.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Product entity
 * Provides data access methods for products
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * Find product by code
     */
    Optional<Product> findByCode(String code);

    /**
     * Check if product exists by code
     */
    boolean existsByCode(String code);

    /**
     * Find all products with pagination
     */
    Page<Product> findAll(Pageable pageable);

    /**
     * Find products by category code with pagination
     */
    @Query("SELECT p FROM Product p WHERE p.category.code = :categoryCode")
    Page<Product> findByCategoryCode(@Param("categoryCode") String categoryCode, Pageable pageable);

    /**
     * Find products by stock status with pagination
     */
    Page<Product> findByIsInStock(boolean isInStock, Pageable pageable);

    /**
     * Find products by category and stock status with pagination
     */
    @Query("SELECT p FROM Product p WHERE p.category.code = :categoryCode AND p.isInStock = :isInStock")
    Page<Product> findByCategoryCodeAndIsInStock(
            @Param("categoryCode") String categoryCode,
            @Param("isInStock") boolean isInStock,
            Pageable pageable
    );

    /**
     * Delete product by code
     */
    void deleteByCode(String code);
}

