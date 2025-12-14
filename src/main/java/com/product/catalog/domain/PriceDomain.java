package com.product.catalog.domain;

import java.math.BigDecimal;
import java.util.Objects;

public class PriceDomain {

    private BigDecimal value;
    private String currency;

    public PriceDomain() {
    }

    public PriceDomain(BigDecimal value, String currency) {
        this.value = value;
        this.currency = currency;
    }

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

    public boolean isValid() {
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0
               && currency != null && currency.length() == 3;
    }

    public String getFormattedPrice() {
        if (value == null || currency == null) {
            return "N/A";
        }
        return String.format("%s %.2f", currency, value);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BigDecimal value;
        private String currency;

        public Builder value(BigDecimal value) {
            this.value = value;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public PriceDomain build() {
            return new PriceDomain(value, currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceDomain that = (PriceDomain) o;
        return Objects.equals(value, that.value) && Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, currency);
    }

    @Override
    public String toString() {
        return "PriceDomain{" +
                "value=" + value +
                ", currency='" + currency + '\'' +
                '}';
    }
}
