package com.product.catalog.service;

import com.product.catalog.domain.ProductDomain;
import com.product.catalog.dto.PatchProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    ProductDomain createProduct(ProductDomain productDomain);

    List<ProductDomain> createProducts(List<ProductDomain> products);

    ProductDomain getProductByCode(String code);

    Page<ProductDomain> getAllProducts(Pageable pageable);

    Page<ProductDomain> getProductsWithFilters(String categoryCode, Boolean inStock, Pageable pageable);

    ProductDomain updateProduct(String code, ProductDomain productDomain);

    ProductDomain patchProduct(String code, PatchProductRequest patchRequest);

    void deleteProduct(String code);

    void deleteProducts(List<String> codes);

    boolean existsByCode(String code);
}

