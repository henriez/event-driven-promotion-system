package com.utfpr.ms_promotion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionDTO(
    Long id,
    String title,
    String description,
    BigDecimal price,
    BigDecimal originalPrice,
    String category,
    String storeId,
    String url,
    LocalDateTime validUntil,
    Integer upvotes
) {}