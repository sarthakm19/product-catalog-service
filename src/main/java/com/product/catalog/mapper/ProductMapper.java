package com.product.catalog.mapper;

import com.product.catalog.domain.ProductDomain;
import com.product.catalog.dto.CreateProductRequest;
import com.product.catalog.dto.ProductResponse;
import com.product.catalog.dto.UpdateProductRequest;
import com.product.catalog.entity.Product;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for Product conversions between layers
 * Uses MapStruct for compile-time generation of mapping code
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {PriceMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductMapper {

    /**
     * Convert Product entity to ProductDomain
     */
    @Mapping(target = "categoryCode", source = "category.code")
    @Mapping(target = "catalogCode", source = "catalog.code")
    @Mapping(target = "isInStock", source = "inStock")
    ProductDomain entityToDomain(Product entity);

    /**
     * Convert ProductDomain to Product entity
     */
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "inStock", source = "isInStock")
    Product domainToEntity(ProductDomain domain);

    /**
     * Convert CreateProductRequest to ProductDomain
     */
    ProductDomain createRequestToDomain(CreateProductRequest request);

    /**
     * Convert UpdateProductRequest to ProductDomain
     */
    ProductDomain updateRequestToDomain(UpdateProductRequest request);

    /**
     * Convert ProductDomain to ProductResponse
     */
    ProductResponse domainToResponse(ProductDomain domain);

    /**
     * Convert Product entity to ProductResponse
     */
    @Mapping(target = "categoryCode", source = "category.code")
    @Mapping(target = "catalogCode", source = "catalog.code")
    @Mapping(target = "isInStock", source = "inStock")
    ProductResponse entityToResponse(Product entity);

    /**
     * Convert CreateProductRequest to Product entity
     */
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "inStock", source = "isInStock")
    Product createRequestToEntity(CreateProductRequest request);

    /**
     * Update entity from UpdateProductRequest
     */
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "inStock", source = "isInStock")
    void updateEntityFromRequest(UpdateProductRequest request, @MappingTarget Product entity);

    /**
     * Partially update entity from domain
     */
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "inStock", source = "isInStock")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDomain(ProductDomain domain, @MappingTarget Product entity);

    /**
     * Convert list of Product entities to list of ProductDomains
     */
    List<ProductDomain> entitiesToDomains(List<Product> entities);

    /**
     * Convert list of ProductDomains to list of ProductResponses
     */
    List<ProductResponse> domainsToResponses(List<ProductDomain> domains);

    /**
     * Convert list of Product entities to list of ProductResponses
     */
    List<ProductResponse> entitiesToResponses(List<Product> entities);
}
