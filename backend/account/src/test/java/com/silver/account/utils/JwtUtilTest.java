package com.silver.account.utils;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {
    JwtUtil jwt(String secret, long expiry) {
        var jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", secret);
        ReflectionTestUtils.setField(jwt, "expiration", expiry);
        return jwt;
    }

    final String secret = "test-only-key-at-least-32-bytes-long-123456";

    @Test
    void valid() {
        var jwt = jwt(secret, 60000);
        String token = jwt.generateToken("writer01");
        assertDoesNotThrow(() -> jwt.validateToken(token));
        assertEquals("writer01", jwt.getUsername(token));
    }

    @Test
    void expired() {
        String token = jwt(secret, -60000).generateToken("writer01");
        assertThrows(JwtException.class, () -> jwt(secret, 60000).validateToken(token));
    }

    @Test
    void wrongKey() {
        String token = jwt(secret, 60000).generateToken("writer01");
        assertThrows(
                JwtException.class,
                () -> jwt("another-test-key-at-least-32-bytes-long", 60000).validateToken(token));
    }

    @Test
    void malformed() {
        assertThrows(JwtException.class, () -> jwt(secret, 60000).validateToken("not-a-jwt"));
    }
}
