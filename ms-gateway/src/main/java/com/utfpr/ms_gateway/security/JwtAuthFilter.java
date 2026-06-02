package com.utfpr.ms_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final PublicKey publicKey;

    public JwtAuthFilter(@Value("${auth.public-key:}") String publicKeyPem) {
        PublicKey parsed = null;
        if (publicKeyPem != null && !publicKeyPem.isBlank()) {
            try {
                String base64 = publicKeyPem
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "");
                byte[] keyBytes = Base64.getDecoder().decode(base64);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory factory = KeyFactory.getInstance("RSA");
                parsed = factory.generatePublic(spec);
            } catch (Exception e) {
                log.warn("Failed to load RSA public key, JWT validation disabled: {}", e.getMessage());
            }
        } else {
            log.warn("auth.public-key not configured, JWT validation disabled");
        }
        this.publicKey = parsed;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (publicKey != null) {
                try {
                    Claims claims = Jwts.parser()
                            .verifyWith(publicKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                    String role = claims.get("role", String.class);
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception e) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Invalid or expired JWT\"}");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/stream") || path.startsWith("/api/auth");
    }
}
