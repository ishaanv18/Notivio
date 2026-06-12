package com.notivio.service;

import com.google.api.services.gmail.model.Message;
import com.notivio.entity.Email;
import com.notivio.entity.GmailToken;
import com.notivio.entity.User;
import com.notivio.gmail.EmailFilter;
import com.notivio.gmail.GmailApiClient;
import com.notivio.repository.EmailRepository;
import com.notivio.repository.GmailTokenRepository;
import com.notivio.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gmail sync service — fetches and stores new emails for a user.
 * Handles incremental sync, deduplication, filtering, and AI re-processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GmailService {

    private final GmailApiClient gmailApiClient;
    private final EmailFilter emailFilter;
    private final EmailRepository emailRepository;
    private final GmailTokenRepository gmailTokenRepository;
    @Lazy private final AiAnalysisService aiAnalysisService;
    private final EncryptionUtil encryptionUtil;

    /**
     * Re-run AI analysis on all emails that previously failed.
     * Handles both:
     *   - emails with isProcessed=false and processingError set (current fix)
     *   - emails with isProcessed=true and processingError set (old bug: catch block wrongly set isProcessed=true)
     */
    @Transactional
    public int reprocessFailedEmails(User user) {
        // Collect all candidates
        List<Email> toRetry = new ArrayList<>();

        // 1. New style: isProcessed=false with error
        toRetry.addAll(emailRepository.findFailedByUserId(user.getId()));

        // 2. Legacy style: isProcessed=true WITH error (old bug marked them as processed even on failure)
        toRetry.addAll(emailRepository.findProcessedWithErrorsByUserId(user.getId()));

        // 3. Any unprocessed without error too
        List<Email> unprocessed = emailRepository.findUnprocessedByUserId(user.getId());
        for (Email e : unprocessed) {
            if (toRetry.stream().noneMatch(r -> r.getId().equals(e.getId()))) {
                toRetry.add(e);
            }
        }

        log.info("Reprocessing {} emails for user: {}", toRetry.size(), user.getEmail());

        for (Email email : toRetry) {
            // Reset so analyzeEmail() will process it
            email.setIsProcessed(false);
            email.setProcessingError(null);
            emailRepository.save(email);
            aiAnalysisService.analyzeEmail(email);
        }
        return toRetry.size();
    }

    /**
     * Sync new emails for a user. Returns count of new emails fetched.
     * @param force if true, resets lastSyncedAt so a full 30-day rescan is performed.
     */
    @Transactional
    public int syncEmails(User user, boolean force) {
        Optional<GmailToken> tokenOpt = gmailTokenRepository.findByUser(user);
        if (tokenOpt.isEmpty()) {
            log.warn("No Gmail token for user: {}", user.getEmail());
            return 0;
        }

        GmailToken token = tokenOpt.get();

        // Force mode: wipe lastSyncedAt so the query becomes newer_than:30d
        if (force) {
            token.setLastSyncedAt(null);
            gmailTokenRepository.save(token);
            log.info("Force rescan: cleared lastSyncedAt for user: {}", user.getEmail());
        }

        int count = 0;

        try {
            String afterTimestamp = token.getLastSyncedAt() != null
                    ? String.valueOf(token.getLastSyncedAt().toEpochSecond())
                    : null;

            String query = emailFilter.buildSearchQuery(afterTimestamp);
            log.info("Gmail query for {}: {}", user.getEmail(), query);
            List<String> messageIds = gmailApiClient.listMessageIds(token, query);

            log.info("Found {} messages for user: {}", messageIds.size(), user.getEmail());

            List<Email> newEmails = new ArrayList<>();

            for (String messageId : messageIds) {
                if (emailRepository.existsByUserIdAndGmailMessageId(user.getId(), messageId)) {
                    log.debug("Email {} already exists, skipping", messageId);
                    continue;
                }

                try {
                    Message message = gmailApiClient.getMessage(token, messageId);
                    Email email = convertToEmail(user, message);

                    boolean relevant = emailFilter.isRelevant(
                            message, email.getSubject(), email.getSnippet());
                    boolean trustedSender = emailFilter.isTrustedSender(email.getSenderEmail());

                    if (relevant || trustedSender) {
                        email = emailRepository.save(email);
                        newEmails.add(email);
                        count++;
                        log.info("Saved relevant email: '{}' from {}",
                                email.getSubject(), email.getSenderEmail());
                    } else {
                        log.debug("Filtered out: '{}'", email.getSubject());
                    }

                } catch (Exception e) {
                    log.error("Failed to process message {}: {}", messageId, e.getMessage());
                }
            }

            token.setLastSyncedAt(ZonedDateTime.now());
            gmailTokenRepository.save(token);

            newEmails.forEach(aiAnalysisService::analyzeEmail);

            log.info("Synced {} new relevant emails for user: {}", count, user.getEmail());

        } catch (Exception e) {
            log.error("Gmail sync failed for user {}: {}", user.getEmail(), e.getMessage(), e);
        }

        return count;
    }

    /** Convenience overload for scheduler (non-forced). */
    @Transactional
    public int syncEmails(User user) {
        return syncEmails(user, false);
    }

    /**
     * Store Gmail OAuth2 tokens for a user.
     */
    @Transactional
    public void saveGmailTokens(User user, String accessToken, String refreshToken,
                                 Long expiresInSeconds) {
        GmailToken token = gmailTokenRepository.findByUser(user)
                .orElse(GmailToken.builder().user(user).build());

        token.setAccessToken(encryptionUtil.encrypt(accessToken));
        if (refreshToken != null) {
            token.setRefreshToken(encryptionUtil.encrypt(refreshToken));
        }
        token.setExpiresAt(ZonedDateTime.now().plusSeconds(
                expiresInSeconds != null ? expiresInSeconds : 3600));

        gmailTokenRepository.save(token);

        // Mark user as Gmail-connected
        user.setGmailConnected(true);
        log.info("Gmail tokens saved for user: {}", user.getEmail());
    }

    /**
     * Update only the refresh token for an existing GmailToken.
     * Called by OAuth2SuccessHandler after login when the refresh token becomes available.
     */
    @Transactional
    public void updateRefreshToken(User user, String refreshToken) {
        gmailTokenRepository.findByUser(user).ifPresentOrElse(token -> {
            token.setRefreshToken(encryptionUtil.encrypt(refreshToken));
            gmailTokenRepository.save(token);
            log.info("Refresh token updated for user: {}", user.getEmail());
        }, () -> log.warn("No GmailToken found to update refresh token for: {}", user.getEmail()));
    }


    public Optional<GmailToken> getToken(User user) {
        return gmailTokenRepository.findByUser(user);
    }

    private Email convertToEmail(User user, Message message) {
        String subject = gmailApiClient.getHeader(message, "Subject");
        String from    = gmailApiClient.getHeader(message, "From");

        String senderEmail = parseSenderEmail(from);
        String bodyPlain = gmailApiClient.extractPlainTextBody(message);

        ZonedDateTime receivedAt = null;
        if (message.getInternalDate() != null) {
            receivedAt = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(message.getInternalDate()),
                    ZoneId.of("UTC"));
        }

        String[] labels = message.getLabelIds() != null
                ? message.getLabelIds().toArray(new String[0])
                : new String[0];

        return Email.builder()
                .user(user)
                .gmailMessageId(message.getId())
                .gmailThreadId(message.getThreadId())
                .subject(subject)
                .sender(from)
                .senderEmail(senderEmail)
                .snippet(message.getSnippet())
                .bodyPlain(bodyPlain)
                .receivedAt(receivedAt)
                .labels(labels)
                .build();
    }

    private String parseSenderEmail(String from) {
        if (from == null) return null;
        int start = from.indexOf('<');
        int end   = from.indexOf('>');
        if (start >= 0 && end > start) {
            return from.substring(start + 1, end).trim();
        }
        return from.trim();
    }
}
