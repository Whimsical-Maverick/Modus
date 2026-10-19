package com.tracker.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Component
public class RefreshCookieService {
    public static final String NAME = "refresh_token";
    private static final String PATH = "/api/v1/auth";

    private final String frontendOrigin;
    private final boolean secure;
    private final long refreshTokenDays;

    public RefreshCookieService(
            @Value("${security.frontend-origin}") String frontendOrigin,
            @Value("${security.refresh-cookie-secure}") boolean secure,
            @Value("${security.jwt.refresh-token-days}") long refreshTokenDays) {
        this.frontendOrigin = frontendOrigin;
        this.secure = secure;
        this.refreshTokenDays = refreshTokenDays;
    }

    public void requireTrustedOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        // A browser POST supplies Origin; non-browser clients such as curl may not.
        if (origin != null && !frontendOrigin.equals(origin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Untrusted request origin");
        }
    }

    public ResponseCookie issue(String rawToken) {
        return base(rawToken).maxAge(Duration.ofDays(refreshTokenDays)).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);
    }
}
