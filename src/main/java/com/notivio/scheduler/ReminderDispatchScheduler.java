package com.notivio.scheduler;

import com.notivio.entity.Reminder;
import com.notivio.service.NotificationService;
import com.notivio.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Checks for due reminders every minute and dispatches push notifications.
 * This is the core of the reminder delivery system.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderDispatchScheduler {

    private final ReminderService reminderService;
    private final NotificationService notificationService;

    @Scheduled(fixedRateString = "${scheduler.reminder-check-interval-seconds:60}000",
               initialDelay = 10000)
    public void dispatchDueReminders() {
        List<Reminder> dueReminders = reminderService.getDueReminders();

        if (dueReminders.isEmpty()) {
            return;
        }

        log.info("Dispatching {} due reminders", dueReminders.size());

        for (Reminder reminder : dueReminders) {
            try {
                // Send push notification
                notificationService.sendReminderNotification(reminder);

                // Mark as sent
                reminderService.markAsSent(reminder);

                log.info("Reminder dispatched: '{}' for task '{}'",
                        reminder.getIntervalLabel(),
                        reminder.getTask().getTitle());

            } catch (Exception e) {
                log.error("Failed to dispatch reminder {}: {}", reminder.getId(), e.getMessage());
                reminderService.markAsFailed(reminder, e.getMessage());
            }
        }
    }
}
