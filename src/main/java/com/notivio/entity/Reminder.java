package com.notivio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Scheduled reminder for a task/deadline.
 * Four reminders are auto-created per task:
 * 1 day before, 6 hours before, 1 hour before, 15 minutes before.
 */
@Entity
@Table(name = "reminders", indexes = {
        @Index(name = "idx_reminders_remind_at", columnList = "remindAt"),
        @Index(name = "idx_reminders_status", columnList = "status"),
        @Index(name = "idx_reminders_user_id", columnList = "userId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private ExtractedTask task;

    @Column(name = "remind_at", nullable = false)
    private ZonedDateTime remindAt;

    /** Human-readable label e.g. "1 day before", "15 minutes before" */
    @Column(name = "interval_label", length = 50)
    private String intervalLabel;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReminderStatus status = ReminderStatus.SCHEDULED;

    @Column(name = "sent_at")
    private ZonedDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public enum ReminderStatus {
        SCHEDULED, SENT, FAILED, CANCELLED, SKIPPED
    }
}
