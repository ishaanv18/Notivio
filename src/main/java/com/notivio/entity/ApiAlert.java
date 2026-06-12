package com.notivio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/** Alert generated when an API key approaches its quota limit. */
@Entity
@Table(name = "api_alerts", indexes = {
        @Index(name = "idx_api_alerts_service", columnList = "service"),
        @Index(name = "idx_api_alerts_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiKey.ApiService service;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_level", nullable = false)
    private AlertLevel alertLevel;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AlertStatus status = AlertStatus.ACTIVE;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "usage_percentage", precision = 5, scale = 2)
    private BigDecimal usagePercentage;

    @Column(name = "tokens_used")
    private Long tokensUsed;

    @Column(name = "tokens_limit")
    private Long tokensLimit;

    @Column(name = "acknowledged_at")
    private ZonedDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private ZonedDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    public enum AlertLevel { WARNING, CRITICAL, INFO }
    public enum AlertStatus { ACTIVE, ACKNOWLEDGED, RESOLVED }
}
