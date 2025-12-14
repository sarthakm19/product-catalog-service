package com.product.catalog.dto;

/**
 * DTO for product response
 */
public class ProductResponse {

    private String code;
    private String name;
    private String description;
    private PriceDto basePrice;
    private Boolean isInStock;
    private String stockKeepingUnit;
    private String categoryCode;
    private String catalogCode;

    public ProductResponse() {}

    public ProductResponse(String code, String name, String description, PriceDto basePrice, Boolean isInStock, String stockKeepingUnit, String categoryCode, String catalogCode) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.isInStock = isInStock;
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
