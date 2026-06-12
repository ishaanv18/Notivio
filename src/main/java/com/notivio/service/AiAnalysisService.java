package com.notivio.service;

import com.notivio.ai.AiPromptBuilder;
import com.notivio.ai.AiResponseParser;
import com.notivio.ai.GroqClient;
import com.notivio.ai.OpenRouterClient;
import com.notivio.entity.*;
import com.notivio.repository.EmailRepository;
import com.notivio.repository.ExtractedTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Core AI analysis service.
 * Orchestrates: email → AI prompt → response parsing → task extraction.
 * Uses Groq as primary, OpenRouter as automatic fallback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final GroqClient groqClient;
    private final OpenRouterClient openRouterClient;
    private final AiPromptBuilder promptBuilder;
    private final AiResponseParser responseParser;
    private final EmailRepository emailRepository;
    private final ExtractedTaskRepository taskRepository;
    private final ReminderService reminderService;
    private final WebSocketNotificationService wsNotificationService;

    /**
     * Analyze a single email and extract tasks if relevant.
     * Called asynchronously from the email sync pipeline.
     */
    @Async("emailProcessingExecutor")
    @Transactional
    public void analyzeEmail(Email email) {
        if (email.getIsProcessed()) {
            log.debug("Email {} already processed, skipping", email.getGmailMessageId());
            return;
        }

        log.info("AI analyzing email: {} from {}", email.getSubject(), email.getSenderEmail());

        try {
            String prompt = promptBuilder.buildExtractionPrompt(
                    email.getSubject(),
                    email.getBodyPlain() != null ? email.getBodyPlain() : email.getSnippet(),
                    email.getSenderEmail()
            );

            // Try Groq first, fallback to OpenRouter
            String aiResponse = callAiWithFallback(prompt, email);

            AiResponseParser.ExtractionResult result = responseParser.parseExtractionResponse(aiResponse);

            email.setIsProcessed(true);
            email.setProcessedAt(ZonedDateTime.now());

            if (result.getIsRelevant() && result.getTitle() != null) {
                email.setIsRelevant(true);

                // Check for duplicate task
                if (isDuplicateTask(email.getUser().getId(), result)) {
                    log.info("Duplicate task detected, skipping: {}", result.getTitle());
                    emailRepository.save(email);
                    return;
                }

                // Save extracted task
                ExtractedTask task = buildTask(email, result);
                task = taskRepository.save(task);

                // Auto-create reminders
                if (task.getDeadline() != null &&
                    task.getDeadline().isAfter(ZonedDateTime.now())) {
                    reminderService.createRemindersForTask(task);
                    task.setIsReminderCreated(true);
                    taskRepository.save(task);
                }

                log.info("Task extracted: '{}' | Priority: {} | Deadline: {}",
                        task.getTitle(), task.getPriority(), task.getDeadline());

                // Broadcast real-time update
                wsNotificationService.broadcastTaskCreated(email.getUser().getId(), task);

            } else {
                log.debug("Email '{}' not relevant for task extraction", email.getSubject());
            }

            emailRepository.save(email);

        } catch (Exception e) {
            log.error("AI analysis failed for email {}: {}", email.getId(), e.getMessage());
            // Do NOT set isProcessed=true — leave it false so the email can be retried
            email.setProcessingError(e.getMessage());
            emailRepository.save(email);
        }
    }

    /**
     * Call AI with automatic Groq → OpenRouter fallback.
     */
    public String callAiWithFallback(String prompt, Email email) {
        // Try Groq first
        if (groqClient.isConfigured()) {
            try {
                return groqClient.complete(prompt, email);
            } catch (Exception e) {
                log.warn("Groq failed, switching to OpenRouter: {}", e.getMessage());
            }
        }

        // Fallback to OpenRouter
        if (openRouterClient.isConfigured()) {
            return openRouterClient.complete(prompt, email);
        }

        throw new RuntimeException("No AI provider configured. Set GROQ_API_KEY or OPENROUTER_API_KEY.");
    }

    /**
     * Generate a text completion without an email context (for digests, etc.)
     */
    public String generateCompletion(String prompt) {
        return callAiWithFallback(prompt, null);
    }

    private boolean isDuplicateTask(java.util.UUID userId,
                                    AiResponseParser.ExtractionResult result) {
        if (result.getDeadline() == null) return false;
        return taskRepository.existsSimilarTask(
                userId,
                result.getTitle(),
                result.getDeadline().minusHours(12),
                result.getDeadline().plusHours(12)
        );
    }

    private ExtractedTask buildTask(Email email, AiResponseParser.ExtractionResult result) {
        return ExtractedTask.builder()
                .user(email.getUser())
                .email(email)
                .title(result.getTitle())
                .description(result.getDescription())
                .taskType(result.getTaskType())
                .priority(result.getPriority())
                .status(ExtractedTask.TaskStatus.PENDING)
                .deadline(result.getDeadline())
                .eventDate(result.getEventDate())
                .location(result.getLocation())
                .organizer(result.getOrganizer())
                .courseName(result.getCourseName())
                .sourceEmailSender(email.getSenderEmail())
                .aiConfidence(result.getAiConfidence())
                .aiSummary(result.getAiSummary())
                .build();
    }
}
