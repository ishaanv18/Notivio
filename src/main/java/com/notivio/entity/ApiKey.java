package com.notivio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/** Tracks API keys and their usage/limit metadata for monitoring. */
@Entity
@Table(name = "api_keys",
        indexes = @Index(name = "idx_api_keys_service", columnList = "service"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiService service;

    @Column(name = "key_name")
    private String keyName;

    @Column(name = "api_key_encrypted", columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @Column(name = "monthly_limit")
    private Integer monthlyLimit;

    @Column(name = "total_requests")
    @Builder.Default
    private Long totalRequests = 0L;

    @Column(name = "total_tokens")
    @Builder.Default
    private Long totalTokens = 0L;

    @Column(name = "last_used_at")
    private ZonedDateTime lastUsedAt;

    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public enum ApiService { GROQ, OPENROUTER, GMAIL, GOOGLE_CALENDAR, FIREBASE }
}
