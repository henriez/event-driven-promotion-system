package com.utfpr.ms_authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @Size(min = 1, max = 100)
    String storeId,

    String signature,

    String timestamp,

    @Email
    String email,

    @Size(min = 6, max = 255)
    String password
) {}
