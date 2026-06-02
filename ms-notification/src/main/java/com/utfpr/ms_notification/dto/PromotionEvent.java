package com.utfpr.ms_notification.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionEvent(
    Long id,
    String title,
    String description,
    BigDecimal price,
    BigDecimal originalPrice,
    String category,
    String storeId,
    String url,
    LocalDateTime validUntil
) {}
