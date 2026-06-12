package com.notivio.scheduler;

import com.notivio.entity.User;
import com.notivio.repository.UserRepository;
import com.notivio.service.GmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically fetches new emails from Gmail for all connected users.
 * Runs every 15 minutes by default (configurable).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSyncScheduler {

    private final GmailService gmailService;
    private final UserRepository userRepository;

    // Default: every 15 minutes
    @Scheduled(fixedRateString = "${gmail.sync-interval-minutes:15}000",
               initialDelay = 30000) // 30s startup delay
    public void syncAllUserEmails() {
        List<User> users = userRepository.findAllActiveGmailConnectedUsers();

        if (users.isEmpty()) {
            log.debug("No Gmail-connected users to sync");
            return;
        }

        log.info("Starting email sync for {} users", users.size());

        int totalNew = 0;
        for (User user : users) {
            try {
                int newEmails = gmailService.syncEmails(user);
                totalNew += newEmails;
            } catch (Exception e) {
                log.error("Email sync failed for user {}: {}", user.getEmail(), e.getMessage());
            }
        }

        log.info("Email sync complete. Total new emails: {}", totalNew);
    }
}
