package com.utfpr.ms_gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromotionDTO(
    @NotBlank 
    @Size(min = 1, max=255)
    String title,

    @NotBlank 
    @Size(min = 1, max=255)
    String category,

    @NotBlank 
    @Size(min = 1, max=255)
    String storeId
){}