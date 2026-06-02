package com.utfpr.ms_authentication.dto;

public record LoginResponse(
    String token,
    String type,
    Long expiresIn
) {}
