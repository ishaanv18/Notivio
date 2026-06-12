package com.notivio.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.notivio.entity.ExtractedTask;
import com.notivio.entity.GmailToken;
import com.notivio.entity.User;
import com.notivio.repository.ExtractedTaskRepository;
import com.notivio.repository.GmailTokenRepository;
import com.notivio.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.TimeZone;

/**
 * Google Calendar integration service.
 * Automatically creates calendar events for extracted tasks/deadlines.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final GmailTokenRepository gmailTokenRepository;
    private final ExtractedTaskRepository taskRepository;
    private final EncryptionUtil encryptionUtil;

    private static final String APPLICATION_NAME = "Notivio";

    /**
     * Create a Google Calendar event for a detected task.
     * Called asynchronously after task extraction.
     */
    @Async("emailProcessingExecutor")
    public void createCalendarEvent(ExtractedTask task) {
        if (task.getDeadline() == null) {
            log.debug("No deadline for task {}, skipping calendar event", task.getId());
            return;
        }

        User user = task.getUser();
        Optional<GmailToken> tokenOpt = gmailTokenRepository.findByUser(user);

        if (tokenOpt.isEmpty()) {
            log.warn("No token for calendar event creation, user: {}", user.getEmail());
            return;
        }

        try {
            GmailToken token = tokenOpt.get();
            Calendar calendarService = buildCalendarService(token);

            Event event = new Event()
                    .setSummary(buildEventSummary(task))
                    .setDescription(buildEventDescription(task))
                    .setLocation(task.getLocation());

            // Set event time based on task type
            DateTime eventTime;
            if (task.getEventDate() != null) {
                eventTime = new DateTime(task.getEventDate().toInstant().toEpochMilli());
            } else {
                eventTime = new DateTime(task.getDeadline().toInstant().toEpochMilli());
            }

            EventDateTime start = new EventDateTime()
                    .setDateTime(eventTime)
                    .setTimeZone(user.getTimezone() != null ? user.getTimezone() : "UTC");

            event.setStart(start);
            event.setEnd(new EventDateTime()
                    .setDateTime(new DateTime(eventTime.getValue() + 3600000)) // +1 hour
                    .setTimeZone(start.getTimeZone()));

            Event created = calendarService.events()
                    .insert("primary", event)
                    .execute();

            // Save calendar event ID to task
            task.setCalendarEventId(created.getId());
            taskRepository.save(task);

            log.info("Calendar event created: {} for task: {}", created.getId(), task.getTitle());

        } catch (Exception e) {
            log.error("Failed to create calendar event for task {}: {}",
                    task.getId(), e.getMessage());
        }
    }

    private Calendar buildCalendarService(GmailToken token) throws Exception {
        String decryptedToken = encryptionUtil.decrypt(token.getAccessToken());

        com.google.auth.oauth2.AccessToken accessToken =
                new com.google.auth.oauth2.AccessToken(decryptedToken,
                        token.getExpiresAt() != null
                                ? java.util.Date.from(token.getExpiresAt().toInstant())
                                : null);

        com.google.auth.oauth2.OAuth2Credentials credentials =
                com.google.auth.oauth2.OAuth2Credentials.create(accessToken);

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new com.google.auth.http.HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private String buildEventSummary(ExtractedTask task) {
        String emoji = switch (task.getTaskType()) {
            case EXAM       -> "📝 Exam: ";
            case ASSIGNMENT -> "📋 Assignment: ";
            case INTERVIEW  -> "💼 Interview: ";
            case MEETING    -> "🤝 Meeting: ";
            case INTERNSHIP -> "🏢 Internship: ";
            default         -> "🔔 ";
        };
        return emoji + task.getTitle();
    }

    private String buildEventDescription(ExtractedTask task) {
        StringBuilder sb = new StringBuilder();
        sb.append("Created by Notivio\n\n");
        if (task.getDescription() != null) sb.append(task.getDescription()).append("\n\n");
        if (task.getCourseName() != null)  sb.append("Course: ").append(task.getCourseName()).append("\n");
        if (task.getOrganizer() != null)   sb.append("Organizer: ").append(task.getOrganizer()).append("\n");
        sb.append("Priority: ").append(task.getPriority().name());
        return sb.toString();
    }
}
