package com.product.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class Price {

    @Column(name = "base_price_value", precision = 19, scale = 2)
    private BigDecimal value;

    @Column(name = "base_price_currency", length = 3)
    private String currency;

    // Default constructor
    public Price() {
    }

    // Constructor with parameters
    public Price(BigDecimal value, String currency) {
        this.value = value;
        this.currency = currency;
    }

    // Convenience constructor with double
    public Price(Double value, String currency) {
        this.value = value != null ? BigDecimal.valueOf(value) : null;
        this.currency = currency;
    }

    // Getters and Setters
    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "Price{" +
                "value=" + value +
                ", currency='" + currency + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Price)) return false;
        Price price = (Price) o;
        return (value != null ? value.equals(price.value) : price.value == null) &&
               (currency != null ? currency.equals(price.currency) : price.currency == null);
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        return result;
    }
}
