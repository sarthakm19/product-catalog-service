package com.product.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new product
 */
public class CreateProductRequest {

    @NotBlank(message = "Product code is required")
    @Size(min = 1, max = 50, message = "Product code must be between 1 and 50 characters")
    private String code;

    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Base price is required")
    private PriceDto basePrice;

    private Boolean isInStock = true;

    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String stockKeepingUnit;

    private String categoryCode;

    private String catalogCode;

    public CreateProductRequest() {}

    public CreateProductRequest(String code, String name, String description, PriceDto basePrice, Boolean isInStock, String stockKeepingUnit, String categoryCode, String catalogCode) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.isInStock = isInStock == null ? true : isInStock;
        this.stockKeepingUnit = stockKeepingUnit;
        this.categoryCode = categoryCode;
        this.catalogCode = catalogCode;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PriceDto getBasePrice() { return basePrice; }
    public void setBasePrice(PriceDto basePrice) { this.basePrice = basePrice; }
    public Boolean getIsInStock() { return isInStock; }
    public void setIsInStock(Boolean isInStock) { this.isInStock = isInStock; }
    public String getStockKeepingUnit() { return stockKeepingUnit; }
    public void setStockKeepingUnit(String stockKeepingUnit) { this.stockKeepingUnit = stockKeepingUnit; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getCatalogCode() { return catalogCode; }
    public void setCatalogCode(String catalogCode) { this.catalogCode = catalogCode; }
}
