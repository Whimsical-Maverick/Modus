package com.tracker.auth;

import com.tracker.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${security.jwt.refresh-token-days}") long refreshTokenDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenDays = refreshTokenDays;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public String create(User user) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        Instant now = Instant.now();

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setUser(user);
        refreshToken.setCreatedAt(now);
        refreshToken.setExpiresAt(
                now.plus(refreshTokenDays, ChronoUnit.DAYS)
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }
}