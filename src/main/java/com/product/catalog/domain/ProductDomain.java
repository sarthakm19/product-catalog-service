package com.product.catalog.domain;

import java.util.Objects;

public class ProductDomain {

    private String code;
    private String name;
    private String description;
    private PriceDomain basePrice;
    private Boolean isInStock;
    private String stockKeepingUnit;
    private String categoryCode;
    private String catalogCode;

    public ProductDomain() {
    }

    public ProductDomain(String code, String name, String description, PriceDomain basePrice, Boolean isInStock, String stockKeepingUnit, String categoryCode, String catalogCode) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.isInStock = isInStock;
        this.stockKeepingUnit = stockKeepingUnit;
        this.categoryCode = categoryCode;
        this.catalogCode = catalogCode;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PriceDomain getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(PriceDomain basePrice) {
        this.basePrice = basePrice;
    }

    public Boolean getIsInStock() {
        return isInStock;
    }

    public void setIsInStock(Boolean inStock) {
        isInStock = inStock;
    }

    public String getStockKeepingUnit() {
        return stockKeepingUnit;
    }

    public void setStockKeepingUnit(String stockKeepingUnit) {
        this.stockKeepingUnit = stockKeepingUnit;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCatalogCode() {
        return catalogCode;
    }

    public void setCatalogCode(String catalogCode) {
        this.catalogCode = catalogCode;
    }

    public boolean isAvailableForPurchase() {
        return isInStock != null && isInStock && basePrice != null && basePrice.isValid();
    }

    public boolean isValid() {
        return code != null && !code.isBlank()
               && name != null && !name.isBlank()
               && basePrice != null && basePrice.isValid();
    }

    public void markOutOfStock() {
        this.isInStock = false;
    }

    public void markInStock() {
        this.isInStock = true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String code;
        private String name;
        private String description;
        private PriceDomain basePrice;
        private Boolean isInStock;
        private String stockKeepingUnit;
        private String categoryCode;
        private String catalogCode;

        public Builder code(String code) { this.code = code; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder basePrice(PriceDomain basePrice) { this.basePrice = basePrice; return this; }
        public Builder isInStock(Boolean isInStock) { this.isInStock = isInStock; return this; }
        public Builder stockKeepingUnit(String sku) { this.stockKeepingUnit = sku; return this; }
        public Builder categoryCode(String categoryCode) { this.categoryCode = categoryCode; return this; }
        public Builder catalogCode(String catalogCode) { this.catalogCode = catalogCode; return this; }

        public ProductDomain build() {
            return new ProductDomain(code, name, description, basePrice, isInStock, stockKeepingUnit, categoryCode, catalogCode);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductDomain that = (ProductDomain) o;
        return Objects.equals(code, that.code) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(basePrice, that.basePrice) && Objects.equals(isInStock, that.isInStock) && Objects.equals(stockKeepingUnit, that.stockKeepingUnit) && Objects.equals(categoryCode, that.categoryCode) && Objects.equals(catalogCode, that.catalogCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, description, basePrice, isInStock, stockKeepingUnit, categoryCode, catalogCode);
    }

    @Override
    public String toString() {
        return "ProductDomain{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", basePrice=" + basePrice +
                ", isInStock=" + isInStock +
                ", stockKeepingUnit='" + stockKeepingUnit + '\'' +
                ", categoryCode='" + categoryCode + '\'' +
                ", catalogCode='" + catalogCode + '\'' +
                '}';
    }
}
