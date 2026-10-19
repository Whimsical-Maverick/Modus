package com.tracker.auth;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceExpirationTests {
    @Test
    void expiredAccessTokenFailsValidation() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        String base64 = Base64.getEncoder().encodeToString(secret);
        JwtService fresh = new JwtService(base64, 15);
        JwtService expired = new JwtService(base64, -1);

        assertTrue(fresh.isValid(fresh.generateAccessToken(UUID.randomUUID())));
        String pastToken = expired.generateAccessToken(UUID.randomUUID());
        assertFalse(fresh.isValid(pastToken));
        assertThrows(io.jsonwebtoken.JwtException.class,
                () -> fresh.extractUserId(pastToken));
    }
}
