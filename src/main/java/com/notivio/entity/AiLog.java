package com.notivio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

/** Logs every AI API call for usage monitoring and cost tracking. */
@Entity
@Table(name = "ai_logs", indexes = {
        @Index(name = "idx_ai_logs_user_id", columnList = "userId"),
        @Index(name = "idx_ai_logs_provider", columnList = "provider"),
        @Index(name = "idx_ai_logs_created_at", columnList = "createdAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id")
    private Email email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiProvider provider;

    @Column(length = 255)
    private String model;

    @Column(name = "prompt_tokens")
    @Builder.Default
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens")
    @Builder.Default
    private Integer completionTokens = 0;

    @Column(name = "total_tokens")
    @Builder.Default
    private Integer totalTokens = 0;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Builder.Default
    private Boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    public enum AiProvider { GROQ, OPENROUTER }
}
