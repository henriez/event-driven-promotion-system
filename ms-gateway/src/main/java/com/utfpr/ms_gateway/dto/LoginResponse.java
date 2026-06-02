package com.utfpr.ms_gateway.dto;

public record LoginResponse(
    String token,
    String type,
    Long expiresIn
) {}
