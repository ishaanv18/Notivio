package com.notivio.service;

import com.notivio.entity.*;
import com.notivio.notification.FcmNotificationSender;
import com.notivio.repository.DeviceTokenRepository;
import com.notivio.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notification service — sends push notifications and tracks delivery.
 * Uses FCM via FcmNotificationSender. Auto-deactivates invalid tokens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final FcmNotificationSender fcmSender;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Send a reminder notification for a specific reminder.
     */
    @Async("notificationExecutor")
    @Transactional
    public void sendReminderNotification(Reminder reminder) {
        ExtractedTask task = reminder.getTask();
        User user = reminder.getUser();

        String title = buildReminderTitle(task, reminder);
        String body  = buildReminderBody(task, reminder);

        Map<String, String> data = Map.of(
                "taskId",    task.getId().toString(),
                "reminderId",reminder.getId().toString(),
                "type",      "REMINDER",
                "deadline",  task.getDeadline() != null ? task.getDeadline().toString() : ""
        );

        sendPushNotification(user, title, body, data,
                Notification.NotificationType.REMINDER, task, reminder);
    }

    /**
     * Send a notification when a new task is detected from email.
     */
    @Async("notificationExecutor")
    @Transactional
    public void sendTaskCreatedNotification(ExtractedTask task) {
        String title = "📋 New Task Detected";
        String body  = String.format("%s - %s", task.getTitle(),
                task.getDeadline() != null ? "Due: " + task.getDeadline().toLocalDate() : "No deadline");

        Map<String, String> data = Map.of(
                "taskId", task.getId().toString(),
                "type",   "TASK_CREATED"
        );

        sendPushNotification(task.getUser(), title, body, data,
                Notification.NotificationType.TASK_CREATED, task, null);
    }

    /**
     * Register a device token for push notifications.
     */
    @Transactional
    public DeviceToken registerDeviceToken(User user, String token,
                                            DeviceToken.DevicePlatform platform,
                                            String deviceName) {
        // Upsert token
        DeviceToken deviceToken = deviceTokenRepository
                .findByUserIdAndToken(user.getId(), token)
                .orElse(DeviceToken.builder().user(user).token(token).build());

        deviceToken.setPlatform(platform);
        deviceToken.setDeviceName(deviceName);
        deviceToken.setIsActive(true);
        deviceToken.setLastUsedAt(ZonedDateTime.now());

        DeviceToken saved = deviceTokenRepository.save(deviceToken);
        log.info("Device token registered for user: {} platform: {}", user.getEmail(), platform);
        return saved;
    }

    @Transactional
    public void deregisterDeviceToken(User user, String token) {
        deviceTokenRepository.deactivateToken(token);
    }

    public Page<Notification> getNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndStatus(
                userId, Notification.NotificationStatus.SENT);
    }

    /**
     * Send a system/digest notification to a user.
     */
    @Async("notificationExecutor")
    @Transactional
    public void sendSystemNotification(User user, String title, String body) {
        sendPushNotification(user, title, body, Map.of("type", "SYSTEM"),
                Notification.NotificationType.SYSTEM, null, null);
    }

    // ── Private helpers ─────────────────────────────────────────

    private void sendPushNotification(User user, String title, String body,
                                       Map<String, String> data,
                                       Notification.NotificationType type,
                                       ExtractedTask task, Reminder reminder) {
        // Save notification record
        Notification notification = Notification.builder()
                .user(user)
                .task(task)
                .reminder(reminder)
                .type(type)
                .title(title)
                .body(body)
                .status(Notification.NotificationStatus.PENDING)
                .build();
        notification = notificationRepository.save(notification);

        // Get active device tokens
        List<DeviceToken> tokens = deviceTokenRepository
                .findByUserIdAndIsActive(user.getId(), true);

        if (tokens.isEmpty()) {
            log.debug("No active device tokens for user: {}", user.getEmail());
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage("No device tokens registered");
            notificationRepository.save(notification);
            return;
        }

        // Convert data map to String map for FCM
        Map<String, String> fcmData = data != null ? data : Map.of();

        // Send via FCM
        List<String> invalidTokens = fcmSender.send(tokens, title, body, fcmData);

        // Deactivate invalid tokens
        invalidTokens.forEach(t -> deviceTokenRepository.deactivateToken(t));

        // Update notification status
        boolean sent = invalidTokens.size() < tokens.size();
        notification.setStatus(sent ? Notification.NotificationStatus.SENT
                                    : Notification.NotificationStatus.FAILED);
        notification.setSentAt(ZonedDateTime.now());
        notificationRepository.save(notification);

        log.info("Notification '{}' sent to user: {}", title, user.getEmail());
    }

    private String buildReminderTitle(ExtractedTask task, Reminder reminder) {
        String emoji = switch (task.getPriority()) {
            case HIGH   -> "🔴";
            case MEDIUM -> "🟡";
            case LOW    -> "🟢";
        };
        return emoji + " Reminder: " + reminder.getIntervalLabel();
    }

    private String buildReminderBody(ExtractedTask task, Reminder reminder) {
        return String.format("%s\nDeadline: %s",
                task.getTitle(),
                task.getDeadline() != null
                        ? task.getDeadline().toLocalDateTime().toString().replace("T", " ")
                        : "No deadline");
    }
}
