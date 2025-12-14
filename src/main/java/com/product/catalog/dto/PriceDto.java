package com.product.catalog.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO for price information
 */
public class PriceDto {

    @NotNull(message = "Price value is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal value;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-character ISO code")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
    private String currency;

    public PriceDto() {}

    public PriceDto(BigDecimal value, String currency) {
        this.value = value;
        this.currency = currency;
    }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
