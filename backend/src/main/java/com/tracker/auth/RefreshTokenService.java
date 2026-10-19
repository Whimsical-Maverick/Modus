package com.tracker.auth;

import com.tracker.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

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

    private String randomToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    public String create(User user) {
        String rawToken = randomToken();

        Instant now = Instant.now();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setUser(user);
        refreshToken.setFamilyId(UUID.randomUUID());
        refreshToken.setCreatedAt(now);
        refreshToken.setExpiresAt(now.plus(refreshTokenDays, ChronoUnit.DAYS));

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String rawToken) {
        RefreshToken current = refreshTokenRepository
                .findByHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = Instant.now();

        if (current.getRevokedAt() != null) {
            // Retain old hashes so reuse is recognizable; revoke this session's
            // active successor. noRollbackFor commits that revocation before 401.
            revokeActiveFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (!current.getExpiresAt().isAfter(now)) {
            current.setRevokedAt(now);
            throw new InvalidRefreshTokenException();
        }

        current.setRevokedAt(now);
        String replacementRaw = randomToken();
        RefreshToken replacement = new RefreshToken();
        replacement.setTokenHash(hash(replacementRaw));
        replacement.setFamilyId(current.getFamilyId());
        replacement.setUser(current.getUser());
        replacement.setCreatedAt(now);
        // The family has an absolute lifetime; rotation does not extend it.
        replacement.setExpiresAt(current.getExpiresAt());
        refreshTokenRepository.save(replacement);
        return new Rotation(current.getUser().getId(), replacementRaw);
    }

    @Transactional
    public void revokeFamily(String rawToken) {
        refreshTokenRepository.findByHashForUpdate(hash(rawToken))
                .ifPresent(token -> revokeActiveFamily(
                        token.getFamilyId(), Instant.now()));
    }

    private void revokeActiveFamily(UUID familyId, Instant now) {
        for (RefreshToken token :
                refreshTokenRepository.findByFamilyIdAndRevokedAtIsNull(familyId)) {
            token.setRevokedAt(now);
        }
    }

    public record Rotation(UUID userId, String refreshToken) {}
}
