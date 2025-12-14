package com.product.catalog.service.impl;

import com.product.catalog.domain.ProductDomain;
import com.product.catalog.dto.PatchProductRequest;
import com.product.catalog.entity.Catalog;
import com.product.catalog.entity.Category;
import com.product.catalog.entity.Product;
import com.product.catalog.exception.BusinessValidationException;
import com.product.catalog.exception.ResourceAlreadyExistsException;
import com.product.catalog.exception.ResourceNotFoundException;
import com.product.catalog.mapper.ProductMapper;
import com.product.catalog.mapper.PriceMapper;
import com.product.catalog.repository.CatalogRepository;
import com.product.catalog.repository.CategoryRepository;
import com.product.catalog.repository.ProductRepository;
import com.product.catalog.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of ProductService
 * Contains business logic for product operations
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CatalogRepository catalogRepository;
    private final ProductMapper productMapper;
    private final PriceMapper priceMapper;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              CatalogRepository catalogRepository,
                              ProductMapper productMapper,
                              PriceMapper priceMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.catalogRepository = catalogRepository;
        this.productMapper = productMapper;
        this.priceMapper = priceMapper;
    }

    @Override
    public ProductDomain createProduct(ProductDomain productDomain) {
        log.info("Creating product with code: {}", productDomain.getCode());

        // Validate business rules
        if (!productDomain.isValid()) {
            throw new BusinessValidationException("Invalid product data");
        }

        // Check if product already exists
        if (productRepository.existsByCode(productDomain.getCode())) {
            throw new ResourceAlreadyExistsException("Product", "code", productDomain.getCode());
        }

        // Convert to entity
        Product product = productMapper.domainToEntity(productDomain);

        // Set relationships
        setProductRelationships(product, productDomain);

        // Save and return
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with code: {}", savedProduct.getCode());

        return productMapper.entityToDomain(savedProduct);
    }

    @Override
    public List<ProductDomain> createProducts(List<ProductDomain> products) {
        log.info("Creating {} products", products.size());

        // Validate all products
        products.forEach(product -> {
            if (!product.isValid()) {
                throw new BusinessValidationException(
                        "Invalid product data for code: " + product.getCode()
                );
            }
            if (productRepository.existsByCode(product.getCode())) {
                throw new ResourceAlreadyExistsException("Product", "code", product.getCode());
            }
        });

        // Convert to entities and set relationships
        List<Product> productEntities = products.stream()
                .map(domain -> {
                    Product entity = productMapper.domainToEntity(domain);
                    setProductRelationships(entity, domain);
                    return entity;
                })
                .collect(Collectors.toList());

        // Save all
        List<Product> savedProducts = productRepository.saveAll(productEntities);
        log.info("Successfully created {} products", savedProducts.size());

        return productMapper.entitiesToDomains(savedProducts);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDomain getProductByCode(String code) {
        log.info("Fetching product with code: {}", code);

        Product product = productRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "code", code));

        return productMapper.entityToDomain(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDomain> getAllProducts(Pageable pageable) {
        log.info("Fetching all products - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Product> productPage = productRepository.findAll(pageable);
        return productPage.map(productMapper::entityToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDomain> getProductsWithFilters(
            String categoryCode,
            Boolean inStock,
            Pageable pageable
    ) {
        log.info("Fetching products with filters - category: {}, inStock: {}, page: {}, size: {}",
                categoryCode, inStock, pageable.getPageNumber(), pageable.getPageSize());

        Page<Product> productPage;

        if (categoryCode != null && inStock != null) {
            productPage = productRepository.findByCategoryCodeAndIsInStock(
                    categoryCode, inStock, pageable
            );
        } else if (categoryCode != null) {
            productPage = productRepository.findByCategoryCode(categoryCode, pageable);
        } else if (inStock != null) {
            productPage = productRepository.findByIsInStock(inStock, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        return productPage.map(productMapper::entityToDomain);
    }

    @Override
    public ProductDomain updateProduct(String code, ProductDomain productDomain) {
        log.info("Updating product with code: {}", code);

        // Validate business rules
        if (!productDomain.isValid()) {
            throw new BusinessValidationException("Invalid product data");
        }

        // Find existing product
        Product existingProduct = productRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "code", code));

        // Update fields
        existingProduct.setName(productDomain.getName());
        existingProduct.setDescription(productDomain.getDescription());
        existingProduct.setBasePrice(productMapper.domainToEntity(productDomain).getBasePrice());
        existingProduct.setInStock(productDomain.getIsInStock());
        existingProduct.setStockKeepingUnit(productDomain.getStockKeepingUnit());

        // Update relationships
        setProductRelationships(existingProduct, productDomain);

        // Save and return
        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product updated successfully with code: {}", updatedProduct.getCode());

        return productMapper.entityToDomain(updatedProduct);
    }

    @Override
    public ProductDomain patchProduct(String code, PatchProductRequest patchRequest) {
        log.info("Partially updating product with code: {}", code);

        // Find existing product
        Product existingProduct = productRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "code", code));

        // Apply partial updates
        if (patchRequest.getName() != null) {
            existingProduct.setName(patchRequest.getName());
        }
        if (patchRequest.getDescription() != null) {
            existingProduct.setDescription(patchRequest.getDescription());
        }
        if (patchRequest.getBasePrice() != null) {
            existingProduct.setBasePrice(
                    priceMapper.dtoToEntity(patchRequest.getBasePrice())
            );
        }
        if (patchRequest.getIsInStock() != null) {
            existingProduct.setInStock(patchRequest.getIsInStock());
        }
        if (patchRequest.getStockKeepingUnit() != null) {
            existingProduct.setStockKeepingUnit(patchRequest.getStockKeepingUnit());
        }

        // Update relationships if provided
        if (patchRequest.getCategoryCode() != null) {
            Category category = categoryRepository.findByCode(patchRequest.getCategoryCode())
                    .orElse(null);
            existingProduct.setCategory(category);
        }
        if (patchRequest.getCatalogCode() != null) {
            Catalog catalog = catalogRepository.findByCode(patchRequest.getCatalogCode())
                    .orElse(null);
            existingProduct.setCatalog(catalog);
        }

        // Save and return
        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product patched successfully with code: {}", updatedProduct.getCode());

        return productMapper.entityToDomain(updatedProduct);
    }

    @Override
    public void deleteProduct(String code) {
        log.info("Deleting product with code: {}", code);

        if (!productRepository.existsByCode(code)) {
            throw new ResourceNotFoundException("Product", "code", code);
        }

        productRepository.deleteByCode(code);
        log.info("Product deleted successfully with code: {}", code);
    }

    @Override
    public void deleteProducts(List<String> codes) {
        log.info("Deleting {} products", codes.size());

        // Validate all products exist
        for (String code : codes) {
            if (!productRepository.existsByCode(code)) {
                throw new ResourceNotFoundException("Product", "code", code);
            }
        }

        // Delete all
        codes.forEach(productRepository::deleteByCode);
        log.info("Successfully deleted {} products", codes.size());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return productRepository.existsByCode(code);
    }

    /**
     * Helper method to set product relationships (category and catalog)
     */
    private void setProductRelationships(Product product, ProductDomain productDomain) {
        // Set category if provided
        if (productDomain.getCategoryCode() != null) {
            Category category = categoryRepository.findByCode(productDomain.getCategoryCode())
                    .orElse(null);
            product.setCategory(category);
        }

        // Set catalog if provided
        if (productDomain.getCatalogCode() != null) {
            Catalog catalog = catalogRepository.findByCode(productDomain.getCatalogCode())
                    .orElse(null);
            product.setCatalog(catalog);
        }
    }
}
