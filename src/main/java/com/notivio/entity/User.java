package com.notivio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Represents an authenticated Notivio user.
 * Users sign in via Google OAuth2 and optionally connect Gmail/Calendar.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_google_id", columnList = "googleId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "google_id", unique = true, nullable = false)
    private String googleId;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    @Column(name = "profile_picture_url", columnDefinition = "TEXT")
    private String profilePictureUrl;

    @Column(length = 100)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * User role — controls access to admin endpoints.
     * Set ROLE_ADMIN manually in the DB for admin users.
     * Default: ROLE_USER
     */
    @Column(length = 32)
    @Builder.Default
    private String role = "ROLE_USER";

    @Column(name = "gmail_connected")
    @Builder.Default
    private Boolean gmailConnected = false;

    @Column(name = "calendar_connected")
    @Builder.Default
    private Boolean calendarConnected = false;

    @Column(name = "digest_enabled")
    @Builder.Default
    private Boolean digestEnabled = true;

    @Column(name = "digest_time")
    @Builder.Default
    private LocalTime digestTime = LocalTime.of(8, 0);

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
