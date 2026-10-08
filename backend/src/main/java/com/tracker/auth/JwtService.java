package com.tracker.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService
{
    private final SecretKey signingKey;
    private final long accessTokenMinutes;
    public JwtService(@Value("${security.jwt.secret}") String base64Secret,
                      @Value("${security.jwt.access-token-minutes}") long accessTokenMinutes){
        this.signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(base64Secret)
        );
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String generateAccessToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(accessTokenMinutes * 60);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public UUID extractUserId(String token) {
        Jws<Claims> parsedToken = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);

        return UUID.fromString(parsedToken.getPayload().getSubject());
    }

    public boolean isValid(String token) {
        try {
            extractUserId(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

