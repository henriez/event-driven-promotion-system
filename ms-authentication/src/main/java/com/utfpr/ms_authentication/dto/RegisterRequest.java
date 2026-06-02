package com.utfpr.ms_authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 1, max = 255)
    String name,

    @NotBlank @Email @Size(max = 255)
    String email,

    @NotBlank @Size(min = 6, max = 255)
    String password
) {}
