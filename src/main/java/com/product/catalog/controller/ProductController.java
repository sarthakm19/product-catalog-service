package com.product.catalog.controller;

import com.product.catalog.domain.ProductDomain;
import com.product.catalog.dto.*;
import com.product.catalog.mapper.ProductMapper;
import com.product.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Product Controller
 * Handles all product-related API endpoints
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product management operations")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    /**
     * Get all products with pagination and filters
     */
    @GetMapping
    @Operation(summary = "Get all products with pagination")
    public ResponseEntity<ProductPageResponse> getAllProducts(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field and direction (e.g., name,asc)")
            @RequestParam(defaultValue = "code,asc") String sort,

            @Parameter(description = "Filter by category code")
            @RequestParam(required = false) String categoryCode,

            @Parameter(description = "Filter by stock availability")
            @RequestParam(required = false) Boolean inStock
    ) {
        log.info("GET /api/v1/products - page: {}, size: {}, categoryCode: {}, inStock: {}",
                page, size, categoryCode, inStock);

        // Parse sort parameter
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        // Get products with filters
        Page<ProductDomain> productPage = productService.getProductsWithFilters(
                categoryCode, inStock, pageable
        );

        // Convert to response
        ProductPageResponse response = new ProductPageResponse(
                productMapper.domainsToResponses(productPage.getContent()),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get product by code
     */
    @GetMapping("/{code}")
    @Operation(summary = "Get product by code")
    public ResponseEntity<ProductResponse> getProductByCode(
            @Parameter(description = "Product code", required = true)
            @PathVariable String code
    ) {
        log.info("GET /api/v1/products/{}", code);

        ProductDomain productDomain = productService.getProductByCode(code);
        ProductResponse response = productMapper.domainToResponse(productDomain);

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new product
     */
    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        log.info("POST /api/v1/products - code: {}", request.getCode());

        ProductDomain productDomain = productMapper.createRequestToDomain(request);
        ProductDomain createdProduct = productService.createProduct(productDomain);
        ProductResponse response = productMapper.domainToResponse(createdProduct);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create multiple products
     */
    @PostMapping("/batch")
    @Operation(summary = "Create multiple products")
    public ResponseEntity<List<ProductResponse>> createProducts(
            @Valid @RequestBody List<CreateProductRequest> requests
    ) {
        log.info("POST /api/v1/products/batch - count: {}", requests.size());

        List<ProductDomain> productDomains = requests.stream()
                .map(productMapper::createRequestToDomain)
                .collect(Collectors.toList());

        List<ProductDomain> createdProducts = productService.createProducts(productDomains);
        List<ProductResponse> responses = productMapper.domainsToResponses(createdProducts);

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Update product by code (full update)
     */
    @PutMapping("/{code}")
    @Operation(summary = "Update product by code")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product code", required = true)
            @PathVariable String code,

            @Valid @RequestBody UpdateProductRequest request
    ) {
        log.info("PUT /api/v1/products/{}", code);

        ProductDomain productDomain = productMapper.updateRequestToDomain(request);
        ProductDomain updatedProduct = productService.updateProduct(code, productDomain);
        ProductResponse response = productMapper.domainToResponse(updatedProduct);

        return ResponseEntity.ok(response);
    }

    /**
     * Partially update product by code
     */
    @PatchMapping("/{code}")
    @Operation(summary = "Partially update product by code")
    public ResponseEntity<ProductResponse> patchProduct(
            @Parameter(description = "Product code", required = true)
            @PathVariable String code,

            @Valid @RequestBody PatchProductRequest request
    ) {
        log.info("PATCH /api/v1/products/{}", code);

        ProductDomain updatedProduct = productService.patchProduct(code, request);
        ProductResponse response = productMapper.domainToResponse(updatedProduct);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete product by code
     */
    @DeleteMapping("/{code}")
    @Operation(summary = "Delete product by code")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product code", required = true)
            @PathVariable String code
    ) {
        log.info("DELETE /api/v1/products/{}", code);

        productService.deleteProduct(code);

        return ResponseEntity.noContent().build();
    }

    /**
     * Delete multiple products
     */
    @DeleteMapping("/batch")
    @Operation(summary = "Delete multiple products by codes")
    public ResponseEntity<Void> deleteProducts(
            @RequestBody List<String> codes
    ) {
        log.info("DELETE /api/v1/products/batch - count: {}", codes.size());

        productService.deleteProducts(codes);

        return ResponseEntity.noContent().build();
    }
}
