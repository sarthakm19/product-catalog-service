package com.product.catalog.repository;

import com.product.catalog.entity.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Catalog entity
 */
@Repository
public interface CatalogRepository extends JpaRepository<Catalog, String> {

    /**
     * Find catalog by code
     */
    Optional<Catalog> findByCode(String code);

    /**
     * Check if catalog exists by code
     */
    boolean existsByCode(String code);
}

