package com.notivio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/** Per-request log for external API usage (Groq, OpenRouter, Gmail). */
@Entity
@Table(name = "api_usage_logs", indexes = {
        @Index(name = "idx_api_usage_service", columnList = "service"),
        @Index(name = "idx_api_usage_created_at", columnList = "createdAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id")
    private ApiKey apiKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiKey.ApiService service;

    @Column(length = 500)
    private String endpoint;

    @Column(name = "tokens_used")
    @Builder.Default
    private Integer tokensUsed = 0;

    @Column(name = "request_count")
    @Builder.Default
    private Integer requestCount = 1;

    @Column(name = "response_code")
    private Integer responseCode;

    @Builder.Default
    private Boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "cost_estimate", precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal costEstimate = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;
}
