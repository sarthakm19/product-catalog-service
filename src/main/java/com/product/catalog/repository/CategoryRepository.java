package com.product.catalog.repository;

import com.product.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Category entity
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    /**
     * Find category by code
     */
    Optional<Category> findByCode(String code);

    /**
     * Check if category exists by code
     */
    boolean existsByCode(String code);
}

