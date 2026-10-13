package com.silver.account.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        return generateToken(username, 0);
    }

    public String generateToken(String username, Integer version) {
        return Jwts.builder()
                .claim("version", version)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public void validateToken(String token) {
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(normalize(token));
    }

    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(normalize(token))
                .getPayload()
                .getSubject();
    }

    public Integer getVersion(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(normalize(token))
                .getPayload()
                .get("version", Integer.class);
    }

    private String normalize(String token) {
        return token != null && token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
