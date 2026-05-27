package com.moneytransfer.auth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private JwtUtil jwtUtil = new JwtUtil(
        "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
        900000, 604800000);

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtUtil.generateAccessToken(1L, "testuser");
        assertNotNull(token);
        var claims = jwtUtil.validateToken(token);
        assertEquals("testuser", claims.getSubject());
        assertEquals(1, (int) claims.get("userId"));
    }

    @Test
    void testExpiredToken() {
        JwtUtil shortJwt = new JwtUtil(
            "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
            1, 1);
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        String token = shortJwt.generateAccessToken(1L, "test");
        assertTrue(shortJwt.isTokenExpired(token));
    }
}
