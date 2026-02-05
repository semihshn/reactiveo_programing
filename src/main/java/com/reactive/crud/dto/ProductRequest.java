package com.reactive.crud.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
        @JsonProperty("name")
        String name,

        @JsonProperty("description")
        String description,

        @NotNull(message = "Product price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
        @JsonProperty("price")
        BigDecimal price
) {
}
