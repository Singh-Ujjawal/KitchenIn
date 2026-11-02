package com.example.kitechin.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    void tokenRoundTripKeepsTheUserIdAndRole() {
        JwtService jwtService = new JwtService("01234567890123456789012345678901", 3_600_000);
        String token = jwtService.generateToken("abc123", "CUSTOMER", "rahul@example.com");

        var claims = jwtService.parse(token);

        assertEquals("abc123", claims.getSubject());
        assertEquals("CUSTOMER", claims.get("role", String.class));
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 3_600_000));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtService("01234567890123456789012345678901", 3_600_000);
        String token = jwtService.generateToken("abc123", "CUSTOMER", "rahul@example.com");
        assertThrows(com.example.kitechin.exception.ApiException.class, () -> jwtService.parse(token + "x"));
    }
}
