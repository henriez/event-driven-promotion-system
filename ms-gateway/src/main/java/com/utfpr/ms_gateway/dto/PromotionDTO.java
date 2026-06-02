package com.utfpr.ms_gateway.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionDTO(
    Long id,

    @NotBlank @Size(min = 1, max = 255)
    String title,

    String description,

    @NotNull @DecimalMin("0.01")
    BigDecimal price,

    @DecimalMin("0.01")
    BigDecimal originalPrice,

    @NotBlank @Size(max = 100)
    String category,

    @NotBlank @Size(max = 100)
    String storeId,

    @NotBlank
    String url,

    LocalDateTime validUntil,

    Integer upvotes
) {}
