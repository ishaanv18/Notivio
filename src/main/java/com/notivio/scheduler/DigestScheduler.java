package com.notivio.scheduler;

import com.notivio.ai.AiPromptBuilder;
import com.notivio.entity.ExtractedTask;
import com.notivio.entity.User;
import com.notivio.repository.UserRepository;
import com.notivio.service.AiAnalysisService;
import com.notivio.service.NotificationService;
import com.notivio.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates and sends daily AI digest to users every morning at 8 AM UTC.
 * Digest includes upcoming tasks, overdue items, and motivational message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DigestScheduler {

    private final UserRepository userRepository;
    private final TaskService taskService;
    private final AiAnalysisService aiAnalysisService;
    private final AiPromptBuilder promptBuilder;
    private final NotificationService notificationService;

    // Every day at 8:00 AM UTC
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyDigests() {
        List<User> users = userRepository.findAllDigestEnabledUsers();
        log.info("Sending daily digest to {} users", users.size());

        for (User user : users) {
            try {
                sendDigestForUser(user);
            } catch (Exception e) {
                log.error("Digest failed for user {}: {}", user.getEmail(), e.getMessage());
            }
        }
    }

    private void sendDigestForUser(User user) {
        List<ExtractedTask> upcoming = taskService.getUpcomingDeadlines(user.getId(), 7);
        List<ExtractedTask> overdue  = taskService.getOverdueTasks(user.getId());

        if (upcoming.isEmpty() && overdue.isEmpty()) {
            log.debug("No tasks for digest, skipping user: {}", user.getEmail());
            return;
        }

        // Build a simple task summary for the AI
        String tasksSummary = buildTasksSummary(upcoming, overdue);
        String prompt = promptBuilder.buildDigestPrompt(tasksSummary);

        String digestText;
        try {
            digestText = aiAnalysisService.generateCompletion(prompt);
        } catch (Exception e) {
            log.warn("AI digest generation failed, using plain summary: {}", e.getMessage());
            digestText = tasksSummary;
        }

        // Send as push notification
        String title = "📅 Your Daily Task Digest";
        notificationService.sendSystemNotification(user, title, digestText);
    }

    private String buildTasksSummary(List<ExtractedTask> upcoming, List<ExtractedTask> overdue) {
        StringBuilder sb = new StringBuilder();

        if (!overdue.isEmpty()) {
            sb.append("OVERDUE (").append(overdue.size()).append("):\n");
            overdue.forEach(t -> sb.append("- ").append(t.getTitle()).append("\n"));
            sb.append("\n");
        }

        if (!upcoming.isEmpty()) {
            sb.append("UPCOMING (next 7 days):\n");
            upcoming.forEach(t -> sb.append("- ").append(t.getTitle())
                    .append(" [").append(t.getDeadline() != null
                            ? t.getDeadline().toLocalDate() : "no date").append("]\n"));
        }

        return sb.toString();
    }
}
