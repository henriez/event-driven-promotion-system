package com.utfpr.ms_gateway.exception;

import java.time.Instant;
import java.util.Map;

public record ValidationErrorResponse(
    Instant timestamp,
    Integer status,
    String error,
    String path,
    Map<String, String> fieldErrors
) {}