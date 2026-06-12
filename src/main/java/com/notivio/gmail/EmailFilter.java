package com.notivio.gmail;

import com.google.api.services.gmail.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Filters emails to find relevant academic/work emails.
 * Excludes promotions, social, spam, and marketing emails.
 */
@Slf4j
@Component
public class EmailFilter {

    // Only reject definite spam/trash at label level.
    // CATEGORY_PROMOTIONS and CATEGORY_SOCIAL are NOT excluded here
    // because many legitimate university/job emails land there.
    private static final Set<String> EXCLUDED_LABELS = Set.of(
            "SPAM", "TRASH"
    );

    private static final List<String> RELEVANT_KEYWORDS = List.of(
            // Academic
            "assignment", "exam", "quiz", "submission", "deadline", "due",
            "attendance", "project", "report", "viva", "practical", "lab",
            "seminar", "workshop", "hackathon", "competition", "contest",
            // Job / Internship
            "interview", "offer", "internship", "placement", "joining", "onboarding",
            "offer letter", "shortlisted", "selected", "application", "apply",
            "rejected", "last date", "hiring", "recruitment", "assessment",
            // Interview-specific (on-site, rounds, etc.)
            "on-site", "onsite", "on site", "face to face", "f2f", "in-person",
            "in person", "technical round", "hr round", "round 1", "round 2",
            "final round", "walk-in", "walk in", "campus drive", "drive",
            "panel interview", "virtual interview", "telephonic", "zoom", "teams",
            // General
            "meeting", "schedule", "reminder", "important", "urgent", "register",
            "event", "register"
    );

    // Only match very specific, high-confidence spam signals in the body.
    // NOTE: Do NOT add 'unsubscribe' here — almost every legitimate transactional
    // email (interview invites, offer letters, etc.) has an unsubscribe link.
    private static final List<Pattern> SPAM_PATTERNS = List.of(
            Pattern.compile("(?i)(limited offer|buy now|exclusive deal|click to (buy|shop)|discount code|promo code)"),
            Pattern.compile("(?i)(congratulations you (have been|are) selected.*free|you('ve| have) won)"),
            Pattern.compile("(?i)(weight loss|make money|work from home.*\\$|free gift|claim your prize)")
    );

    // Domains/senders that are definitely spam-only (checked on sender email, not body)
    private static final List<Pattern> SPAM_SENDER_PATTERNS = List.of(
            Pattern.compile("(?i)(newsletter@|marketing@|promotions@|ads@|spam@)")
    );

    private static final List<String> TRUSTED_SENDER_DOMAINS = List.of(
            ".edu", ".ac.in", ".ac.uk", "university", "college", "institute",
            "campus", "school", "noreply@linkedin", "jobs@", "careers@",
            "internshala", "indeed", "glassdoor", "naukri", "hr@",
            "instahyre", "hackerearth", "unstop", "dare2compete", "cutshort",
            "angellist", "wellfound", "greenhouse", "lever", "workday", "taleo"
    );

    @Value("${gmail.max-emails-per-sync:50}")
    private Integer maxEmailsPerSync;

    /**
     * Build Gmail search query for fetching relevant emails.
     */
    public String buildSearchQuery(String afterTimestamp) {
        // On initial sync (no lastSyncedAt) look back 30 days.
        // Do NOT exclude categories at the API level — Gmail categorises
        // many university/job emails as "Updates" or even "Promotions".
        // We rely on isRelevant() keyword matching to filter noise locally.
        String timeFilter = afterTimestamp != null
                ? "after:" + afterTimestamp + " "
                : "newer_than:30d ";

        return timeFilter + "-in:spam -in:trash";
    }

    /**
     * Determine if a Gmail message is relevant for AI processing.
     * Uses label check + keyword matching + spam pattern detection.
     */
    public boolean isRelevant(Message message, String subject, String bodySnippet) {
        // Reject excluded labels
        if (message.getLabelIds() != null) {
            for (String label : message.getLabelIds()) {
                if (EXCLUDED_LABELS.contains(label)) {
                    log.debug("Email rejected — excluded label: {}", label);
                    return false;
                }
            }
        }

        String combined = ((subject != null ? subject : "") + " " +
                           (bodySnippet != null ? bodySnippet : "")).toLowerCase();

        // Reject obvious spam body patterns
        for (Pattern pattern : SPAM_PATTERNS) {
            if (pattern.matcher(combined).find()) {
                log.debug("Email rejected — spam pattern matched");
                return false;
            }
        }

        // Accept if any relevant keyword found
        for (String keyword : RELEVANT_KEYWORDS) {
            if (combined.contains(keyword)) {
                log.debug("Email accepted — keyword '{}' matched", keyword);
                return true;
            }
        }

        return false;
    }

    /**
     * Check if an email sender is from a trusted educational/work domain.
     */
    public boolean isTrustedSender(String senderEmail) {
        if (senderEmail == null) return false;
        String lower = senderEmail.toLowerCase();
        // First check if it's a known spam sender
        for (Pattern p : SPAM_SENDER_PATTERNS) {
            if (p.matcher(lower).find()) return false;
        }
        return TRUSTED_SENDER_DOMAINS.stream().anyMatch(lower::contains);
    }
}
