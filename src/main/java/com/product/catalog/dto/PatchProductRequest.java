package com.product.catalog.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for partially updating a product (partial update)
 */
public class PatchProductRequest {

    @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private PriceDto basePrice;

    private Boolean isInStock;

    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String stockKeepingUnit;

    private String categoryCode;

    private String catalogCode;

    public PatchProductRequest() {}

    public PatchProductRequest(String name, String description, PriceDto basePrice, Boolean isInStock, String stockKeepingUnit, String categoryCode, String catalogCode) {
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.isInStock = isInStock;
        this.stockKeepingUnit = stockKeepingUnit;
        this.categoryCode = categoryCode;
        this.catalogCode = catalogCode;
    }

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
