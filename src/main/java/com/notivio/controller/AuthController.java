package com.notivio.controller;

import com.notivio.entity.User;
import com.notivio.repository.UserRepository;
import com.notivio.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Authentication-related endpoints.
 * OAuth2 login is handled by Spring Security at /oauth2/authorization/google.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and user profile")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository   userRepository;

    /**
     * Exchange the short-lived OAuth2 exchange code for real JWT tokens.
     *
     * STATELESS DESIGN: The exchange code is a signed JWT (type="exchange", 2-min TTL).
     * - No in-memory store needed — the signature IS the proof of authenticity.
     * - React StrictMode safe: double calls just produce fresh tokens from the same userId.
     * - The exchange code only encodes a userId, so even if it leaks from a URL it's
     *   harmless after the 2-minute window expires.
     */
    @PostMapping("/exchange")
    @Operation(summary = "Exchange one-time OAuth2 code for JWT tokens")
    public ResponseEntity<Map<String, Object>> exchangeCode(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing code"));
        }

        // Validate the signed exchange code and extract userId
        UUID userId = jwtTokenProvider.validateExchangeCode(code);
        if (userId == null) {
            log.warn("Invalid or expired exchange code");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired code. Please login again."));
        }

        // Look up the user
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.getIsActive()) {
            log.warn("Exchange code valid but user {} not found or inactive", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User account not found or deactivated."));
        }

        // Issue fresh real tokens
        String accessToken  = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.info("Tokens issued via exchange for user: {}", user.getEmail());

        return ResponseEntity.ok(Map.of(
                "accessToken",  accessToken,
                "refreshToken", refreshToken
        ));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<Map<String, Object>> getMe(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "id",               user.getId(),
                "email",            user.getEmail(),
                "name",             user.getName() != null ? user.getName() : "",
                "profilePicture",   user.getProfilePictureUrl() != null ? user.getProfilePictureUrl() : "",
                "timezone",         user.getTimezone(),
                "gmailConnected",   user.getGmailConnected(),
                "calendarConnected",user.getCalendarConnected(),
                "digestEnabled",    user.getDigestEnabled(),
                "createdAt",        user.getCreatedAt()
        ));
    }

    @GetMapping("/login")
    @Operation(summary = "Get Google OAuth2 login URL")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        return ResponseEntity.ok(Map.of(
                "loginUrl",    "/oauth2/authorization/google",
                "description", "Redirect user to this URL to initiate Google OAuth2 login"
        ));
    }
}
