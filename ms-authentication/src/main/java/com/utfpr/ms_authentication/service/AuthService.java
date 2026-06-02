package com.utfpr.ms_authentication.service;

import com.utfpr.ms_authentication.dto.LoginRequest;
import com.utfpr.ms_authentication.dto.LoginResponse;
import com.utfpr.ms_authentication.dto.RegisterRequest;
import com.utfpr.ms_authentication.entity.StoreCredential;
import com.utfpr.ms_authentication.entity.User;
import com.utfpr.ms_authentication.repository.StoreCredentialRepository;
import com.utfpr.ms_authentication.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StoreCredentialRepository storeCredentialRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, StoreCredentialRepository storeCredentialRepository, JwtService jwtService, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.storeCredentialRepository = storeCredentialRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse authenticate(LoginRequest request) {
        if (request.storeId() != null && !request.storeId().isBlank()) {
            return authenticateStore(request.storeId(), request.signature(), request.timestamp());
        }
        if (request.email() != null && request.password() != null) {
            return authenticateUser(request.email(), request.password());
        }
        throw new IllegalArgumentException("Invalid login credentials");
    }

    private LoginResponse authenticateStore(String storeId, String signature, String timestamp) {
        if (signature == null || signature.isBlank() || timestamp == null || timestamp.isBlank()) {
            throw new IllegalArgumentException("Store authentication requires signature and timestamp");
        }

        StoreCredential store = storeCredentialRepository.findByStoreIdAndIsActiveTrue(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found or inactive"));

        try {
            String base64key = store.getPublicKey()
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(base64key);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey storePublicKey = keyFactory.generatePublic(keySpec);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(storePublicKey);
            sig.update(timestamp.getBytes(StandardCharsets.UTF_8));

            if (!sig.verify(Base64.getDecoder().decode(signature))) {
                throw new IllegalArgumentException("Invalid store signature");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Signature verification failed", e);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "STORE");
        claims.put("storeId", store.getStoreId());

        String token = jwtService.generateToken(claims, store.getStoreId());
        return new LoginResponse(token, "Bearer", 86400L);
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setIsActive(true);
        user = userRepository.save(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());

        String token = jwtService.generateToken(claims, user.getId().toString());
        return new LoginResponse(token, "Bearer", 86400L);
    }

    private LoginResponse authenticateUser(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());

        String token = jwtService.generateToken(claims, user.getId().toString());
        return new LoginResponse(token, "Bearer", 86400L);
    }
}
