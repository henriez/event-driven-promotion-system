package com.utfpr.ms_gateway.controller;

import com.utfpr.ms_gateway.dto.LoginRequest;
import com.utfpr.ms_gateway.dto.LoginResponse;
import com.utfpr.ms_gateway.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthProxyController {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public AuthProxyController(RestTemplate restTemplate,
                               @Value("${auth.service.url}") String authServiceUrl) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                authServiceUrl + "/api/auth/register", request, LoginResponse.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(response.getBody());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                authServiceUrl + "/api/auth/login", request, LoginResponse.class);
        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/public-key")
    public ResponseEntity<String> getPublicKey() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                authServiceUrl + "/api/auth/public-key", String.class);
        return ResponseEntity.ok(response.getBody());
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<String> handleAuthError(HttpStatusCodeException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleConnectionError(ResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
            "timestamp", Instant.now(),
            "status", HttpStatus.BAD_GATEWAY.value(),
            "error", "Bad Gateway",
            "message", "Unable to reach authentication service at " + authServiceUrl
        ));
    }
}
