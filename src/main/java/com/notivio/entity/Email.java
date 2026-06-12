package com.notivio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Stores fetched Gmail messages with their content for AI processing.
 * Unique constraint on (user_id, gmail_message_id) prevents duplicate processing.
 */
@Entity
@Table(name = "emails",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_emails_user_message",
                columnNames = {"user_id", "gmail_message_id"}
        ),
        indexes = {
                @Index(name = "idx_emails_user_id", columnList = "userId"),
                @Index(name = "idx_emails_is_processed", columnList = "isProcessed"),
                @Index(name = "idx_emails_received_at", columnList = "receivedAt")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "gmail_message_id", nullable = false)
    private String gmailMessageId;

    @Column(name = "gmail_thread_id")
    private String gmailThreadId;

    @Column(columnDefinition = "TEXT")
    private String subject;

    @Column(length = 500)
    private String sender;

    @Column(name = "sender_email")
    private String senderEmail;

    /** Short preview snippet from Gmail API */
    @Column(columnDefinition = "TEXT")
    private String snippet;

    @Column(name = "body_plain", columnDefinition = "TEXT")
    private String bodyPlain;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "received_at")
    private ZonedDateTime receivedAt;

    /** Gmail label IDs e.g. INBOX, IMPORTANT, CATEGORY_PERSONAL */
    @Column(columnDefinition = "text[]")
    private String[] labels;

    @Column(name = "is_processed")
    @Builder.Default
    private Boolean isProcessed = false;

    /** True if AI determined this email contains a relevant task/deadline */
    @Column(name = "is_relevant")
    @Builder.Default
    private Boolean isRelevant = false;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "processed_at")
    private ZonedDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;
}
