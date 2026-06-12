package com.notivio.service;

import com.notivio.entity.ExtractedTask;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Real-time WebSocket notification service.
 * Broadcasts task events to connected clients via STOMP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastTaskCreated(UUID userId, ExtractedTask task) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/tasks",
                    Map.of(
                            "event", "TASK_CREATED",
                            "taskId", task.getId().toString(),
                            "title", task.getTitle(),
                            "priority", task.getPriority().name(),
                            "deadline", task.getDeadline() != null ? task.getDeadline().toString() : null
                    )
            );
            log.debug("WebSocket task created event sent for user: {}", userId);
        } catch (Exception e) {
            log.warn("WebSocket broadcast failed: {}", e.getMessage());
        }
    }

    public void broadcastReminderFired(UUID userId, String taskTitle, String interval) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/reminders",
                    Map.of(
                            "event", "REMINDER_FIRED",
                            "taskTitle", taskTitle,
                            "interval", interval
                    )
            );
        } catch (Exception e) {
            log.warn("WebSocket reminder broadcast failed: {}", e.getMessage());
        }
    }
}
