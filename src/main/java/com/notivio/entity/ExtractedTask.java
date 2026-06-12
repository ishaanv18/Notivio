package com.notivio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Represents an AI-extracted task or deadline from an email.
 * This is the core entity of the Notivio system.
 */
@Entity
@Table(name = "extracted_tasks", indexes = {
        @Index(name = "idx_tasks_user_id", columnList = "userId"),
        @Index(name = "idx_tasks_deadline", columnList = "deadline"),
        @Index(name = "idx_tasks_status", columnList = "status"),
        @Index(name = "idx_tasks_user_status", columnList = "userId, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtractedTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id")
    private Email email;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type")
    @Builder.Default
    private TaskType taskType = TaskType.OTHER;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    /** Primary deadline — when the task is due */
    private ZonedDateTime deadline;

    /** For events (exams, meetings) — when it actually happens */
    @Column(name = "event_date")
    private ZonedDateTime eventDate;

    @Column(length = 500)
    private String location;

    @Column(length = 255)
    private String organizer;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "source_email_sender")
    private String sourceEmailSender;

    /** AI confidence score 0-100 */
    @Column(name = "ai_confidence", precision = 5, scale = 2)
    private BigDecimal aiConfidence;

    /** AI-generated short summary of the task */
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    /** Google Calendar event ID after calendar sync */
    @Column(name = "calendar_event_id")
    private String calendarEventId;

    @Column(name = "is_reminder_created")
    @Builder.Default
    private Boolean isReminderCreated = false;

    /** Flag for duplicate deadline detection */
    @Column(name = "is_duplicate")
    @Builder.Default
    private Boolean isDuplicate = false;

    @Column(name = "raw_ai_response", columnDefinition = "TEXT")
    private String rawAiResponse;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    // ── Enums ─────────────────────────────────────────────────

    public enum TaskType {
        ASSIGNMENT, EXAM, INTERVIEW, MEETING,
        EVENT, INTERNSHIP, PLACEMENT, SUBMISSION,
        DEADLINE, GENERAL_REMINDER, OTHER
    }

    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    public enum TaskStatus {
        PENDING, IN_PROGRESS, COMPLETED, OVERDUE, CANCELLED
    }
}
