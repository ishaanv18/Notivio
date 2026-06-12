package com.notivio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Stores encrypted Gmail OAuth2 tokens per user.
 * access_token and refresh_token are AES-256-GCM encrypted before storage.
 */
@Entity
@Table(name = "gmail_tokens", indexes = {
        @Index(name = "idx_gmail_tokens_user_id", columnList = "userId"),
        @Index(name = "idx_gmail_tokens_expires_at", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GmailToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** AES-256 encrypted access token */
    @Column(name = "access_token", columnDefinition = "TEXT", nullable = false)
    private String accessToken;

    /** AES-256 encrypted refresh token */
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_type")
    @Builder.Default
    private String tokenType = "Bearer";

    @Column(columnDefinition = "TEXT")
    private String scope;

    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;

    @Column(name = "last_synced_at")
    private ZonedDateTime lastSyncedAt;

    /** Gmail API history ID for incremental sync — avoids re-fetching all emails */
    @Column(name = "last_sync_history_id")
    private String lastSyncHistoryId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public boolean isExpired() {
        return expiresAt != null && ZonedDateTime.now().isAfter(expiresAt);
    }

    public boolean needsRefresh() {
        return expiresAt != null && ZonedDateTime.now().plusMinutes(5).isAfter(expiresAt);
    }
}
