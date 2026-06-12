package com.notivio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

/** FCM / Expo device token registered per user device. */
@Entity
@Table(name = "device_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_device_tokens", columnNames = {"user_id", "token"}
        ),
        indexes = @Index(name = "idx_device_tokens_user_id", columnList = "userId"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DevicePlatform platform = DevicePlatform.ANDROID;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_used_at")
    private ZonedDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public enum DevicePlatform { ANDROID, IOS, WEB }
}
