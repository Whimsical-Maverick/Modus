package com.tracker.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.FORBIDDEN;

class RefreshCookieServiceTests {
    private final RefreshCookieService service =
            new RefreshCookieService("http://localhost:5173", false, 30);

    @Test
    void refreshCookieIsHttpOnlyAndStrict() {
        String cookie = service.issue("secret").toString();

        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Strict"));
        assertTrue(cookie.contains("Path=/api/v1/auth"));
        assertTrue(service.clear().toString().contains("Max-Age=0"));
    }

    @Test
    void browserRequestFromDifferentOriginIsRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://evil.example");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requireTrustedOrigin(request));
        assertEquals(FORBIDDEN, error.getStatusCode());
    }
}
