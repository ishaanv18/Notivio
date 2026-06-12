package com.notivio.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for short-lived OAuth2 state codes.
 *
 * Each code is valid for 60 seconds and is deleted immediately after use
 * (one-time use), preventing token replay attacks.
 *
 * For multi-instance deployments, replace with a Redis-backed store.
 */
@Slf4j
@Component
public class AuthStateStore {

    private static final long TTL_SECONDS = 60;

    private record Entry(String accessToken, String refreshToken, Instant expiresAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /** Store a code → token pair. The code expires in 60 seconds. */
    public void store(String code, String accessToken, String refreshToken) {
        purgeExpired();
        store.put(code, new Entry(accessToken, refreshToken, Instant.now().plusSeconds(TTL_SECONDS)));
    }

    /**
     * Consume a code and return its tokens.
     * Returns null if the code is invalid or expired.
     * The code is deleted after a single use (one-time token).
     */
    public String[] consume(String code) {
        Entry entry = store.remove(code);
        if (entry == null) {
            log.warn("Auth code not found: {}", code);
            return null;
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            log.warn("Auth code expired: {}", code);
            return null;
        }
        return new String[]{ entry.accessToken(), entry.refreshToken() };
    }

    /** Remove all entries that have passed their TTL. */
    private void purgeExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}
