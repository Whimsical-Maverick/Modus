package com.tracker.auth;

import com.tracker.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTests {
    @Mock RefreshTokenRepository repository;
    private RefreshTokenService service;
    private RefreshToken current;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, 30);
        User user = new User();
        user.setId(UUID.randomUUID());
        current = new RefreshToken();
        current.setId(UUID.randomUUID());
        current.setUser(user);
        current.setFamilyId(UUID.randomUUID());
        current.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
    }

    @Test
    void rotationRevokesOldTokenAndPreservesFamilyDeadline() {
        when(repository.findByHashForUpdate(any())).thenReturn(Optional.of(current));

        var rotated = service.rotate("presented-secret");

        assertEquals(current.getUser().getId(), rotated.userId());
        assertNotNull(current.getRevokedAt());
        org.mockito.ArgumentCaptor<RefreshToken> saved =
                org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertEquals(current.getFamilyId(), saved.getValue().getFamilyId());
        assertEquals(current.getExpiresAt(), saved.getValue().getExpiresAt());
        assertEquals(64, saved.getValue().getTokenHash().length());
        // Only the SHA-256 hash, never the raw token, is persisted.
        org.junit.jupiter.api.Assertions.assertNotEquals(
                rotated.refreshToken(), saved.getValue().getTokenHash());
    }

    @Test
    void replayRevokesActiveSuccessor() {
        current.setRevokedAt(Instant.now().minusSeconds(30));
        RefreshToken successor = new RefreshToken();
        successor.setFamilyId(current.getFamilyId());
        when(repository.findByHashForUpdate(any())).thenReturn(Optional.of(current));
        when(repository.findByFamilyIdAndRevokedAtIsNull(current.getFamilyId()))
                .thenReturn(List.of(successor));

        assertThrows(InvalidRefreshTokenException.class,
                () -> service.rotate("used-secret"));

        assertNotNull(successor.getRevokedAt());
        verify(repository, never()).save(any());
    }

    @Test
    void expiredTokenCannotRotate() {
        current.setExpiresAt(Instant.now().minusSeconds(1));
        when(repository.findByHashForUpdate(any())).thenReturn(Optional.of(current));

        assertThrows(InvalidRefreshTokenException.class,
                () -> service.rotate("expired-secret"));

        assertNotNull(current.getRevokedAt());
        verify(repository, never()).save(any());
    }

    @Test
    void logoutRevokesOnlyPresentedFamily() {
        RefreshToken successor = new RefreshToken();
        successor.setFamilyId(current.getFamilyId());
        when(repository.findByHashForUpdate(any())).thenReturn(Optional.of(current));
        when(repository.findByFamilyIdAndRevokedAtIsNull(current.getFamilyId()))
                .thenReturn(List.of(successor));

        service.revokeFamily("presented-secret");

        assertNotNull(successor.getRevokedAt());
    }
}
