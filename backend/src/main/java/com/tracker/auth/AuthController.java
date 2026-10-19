package com.tracker.auth;

import com.tracker.auth.dto.AuthResponse;
import com.tracker.auth.dto.AccessTokenResponse;
import com.tracker.auth.dto.LoginRequest;
import com.tracker.auth.dto.RegisterRequest;
import com.tracker.user.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshCookieService refreshCookieService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        refreshCookieService.requireTrustedOrigin(servletRequest);
        AuthResponse tokens = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookieService.issue(tokens.refreshToken()).toString())
                .body(new AccessTokenResponse(tokens.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @org.springframework.web.bind.annotation.CookieValue(
                    value = RefreshCookieService.NAME, required = false) String rawToken,
            HttpServletRequest servletRequest
    ) {
        refreshCookieService.requireTrustedOrigin(servletRequest);
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        AuthResponse tokens = authService.refresh(rawToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookieService.issue(tokens.refreshToken()).toString())
                .body(new AccessTokenResponse(tokens.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @org.springframework.web.bind.annotation.CookieValue(
                    value = RefreshCookieService.NAME, required = false) String rawToken,
            HttpServletRequest servletRequest
    ) {
        refreshCookieService.requireTrustedOrigin(servletRequest);
        authService.logout(rawToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookieService.clear().toString())
                .build();
    }
}
