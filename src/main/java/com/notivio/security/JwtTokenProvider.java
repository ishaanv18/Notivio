package com.notivio.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Handles JWT token generation, validation, and claims extraction.
 * Uses HMAC-SHA256 with a configurable secret key.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey getSigningKey() {
        // SECURITY FIX: The previous code double-base64-encoded the secret which
        // effectively weakened the key. Now we derive a proper 256-bit key from the
        // raw secret bytes using SHA-256 so the key is always exactly 256 bits.
        try {
            byte[] keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keyBytes);
            return Keys.hmacShaKeyFor(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Generate access JWT token for an authenticated user.
     */
    public String generateToken(UUID userId, String email) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate long-lived refresh token.
     */
    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate a short-lived (2 min) stateless exchange code JWT.
     * Contains only the userId and type="exchange".
     * The /api/auth/exchange endpoint validates this and issues real tokens.
     *
     * Stateless design: no in-memory store needed — the signed JWT IS the proof.
     * React StrictMode double-invokes effects; this is safe because each validation
     * produces fresh tokens rather than consuming a one-time-use entry.
     */
    public String generateExchangeCode(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "exchange")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 120_000)) // 2 minutes
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate an exchange code and return the userId it encodes.
     * Returns null if the code is invalid, expired, or not of type "exchange".
     */
    public UUID validateExchangeCode(String code) {
        try {
            Claims claims = parseClaims(code);
            String type = claims.get("type", String.class);
            if (!"exchange".equals(type)) {
                log.warn("Exchange code has wrong type: {}", type);
                return null;
            }
            return UUID.fromString(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.warn("Exchange code expired");
            return null;
        } catch (Exception e) {
            log.warn("Invalid exchange code: {}", e.getMessage());
            return null;
        }
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}
