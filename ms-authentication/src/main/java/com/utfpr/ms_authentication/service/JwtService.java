package com.utfpr.ms_authentication.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtService(@Value("${auth.private-key}") String privateKeyPem) {
        try {
            String base64 = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PrivateKey pk = factory.generatePrivate(spec);
            RSAPrivateCrtKey rsaPk = (RSAPrivateCrtKey) pk;
            RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(rsaPk.getModulus(), rsaPk.getPublicExponent());
            this.privateKey = pk;
            this.publicKey = factory.generatePublic(pubSpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load RSA private key from configuration", e);
        }
    }

    public String generateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000L))
                .signWith(privateKey)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getPublicKeyPem() {
        byte[] keyBytes = publicKey.getEncoded();
        String base64 = Base64.getMimeEncoder(64, System.lineSeparator().getBytes()).encodeToString(keyBytes);
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }
}
