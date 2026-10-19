package com.tracker.auth;

import com.tracker.auth.dto.AuthResponse;
import com.tracker.auth.dto.LoginRequest;
import com.tracker.auth.dto.RegisterRequest;
import com.tracker.user.User;
import com.tracker.user.UserRepository;
import com.tracker.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public UserResponse register(RegisterRequest  request)
    {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if(userRepository.existsByEmail(email))
        {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUsername(request.username().trim());

        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException(email);
        }
        String id = saved.getId().toString();
        return new UserResponse(id, saved.getEmail(), saved.getUsername());

    }

    public AuthResponse login(LoginRequest request)
    {
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = refreshTokenService.create(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation =
                refreshTokenService.rotate(rawRefreshToken);
        return new AuthResponse(
                jwtService.generateAccessToken(rotation.userId()),
                rotation.refreshToken());
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeFamily(rawRefreshToken);
        }
    }
}
