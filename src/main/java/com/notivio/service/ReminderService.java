package com.notivio.service;

import com.notivio.entity.Reminder;
import com.notivio.entity.ExtractedTask;
import com.notivio.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages reminder lifecycle for tasks.
 * Auto-creates 4 reminders per task: 1 day, 6 hours, 1 hour, 15 min before deadline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;

    /** Reminder intervals before deadline */
    private static final List<ReminderInterval> INTERVALS = List.of(
            new ReminderInterval(24 * 60, "1 day before"),
            new ReminderInterval(6 * 60, "6 hours before"),
            new ReminderInterval(60, "1 hour before"),
            new ReminderInterval(15, "15 minutes before")
    );

    /**
     * Create 4 scheduled reminders for a task.
     * Skips any interval already in the past.
     */
    @Transactional
    public List<Reminder> createRemindersForTask(ExtractedTask task) {
        if (task.getDeadline() == null) {
            log.warn("No deadline for task {}, cannot create reminders", task.getId());
            return List.of();
        }

        ZonedDateTime now = ZonedDateTime.now();
        List<Reminder> created = new ArrayList<>();

        for (ReminderInterval interval : INTERVALS) {
            ZonedDateTime remindAt = task.getDeadline().minusMinutes(interval.minutesBefore());

            // Skip if reminder time is already past
            if (remindAt.isBefore(now)) {
                log.debug("Skipping past reminder '{}' for task: {}", interval.label(), task.getTitle());
                continue;
            }

            // Avoid duplicate reminders for same interval
            if (reminderRepository.existsByTaskIdAndIntervalLabel(task.getId(), interval.label())) {
                log.debug("Reminder '{}' already exists for task: {}", interval.label(), task.getTitle());
                continue;
            }

            Reminder reminder = Reminder.builder()
                    .user(task.getUser())
                    .task(task)
                    .remindAt(remindAt)
                    .intervalLabel(interval.label())
                    .status(Reminder.ReminderStatus.SCHEDULED)
                    .build();

            created.add(reminderRepository.save(reminder));
            log.info("Reminder scheduled: '{}' at {} for task '{}'",
                    interval.label(), remindAt, task.getTitle());
        }

        return created;
    }

    @Transactional
    public void cancelRemindersForTask(UUID taskId) {
        List<Reminder> reminders = reminderRepository.findByTaskId(taskId);
        reminders.stream()
                .filter(r -> r.getStatus() == Reminder.ReminderStatus.SCHEDULED)
                .forEach(r -> {
                    r.setStatus(Reminder.ReminderStatus.CANCELLED);
                    reminderRepository.save(r);
                });
        log.info("Cancelled {} reminders for task {}", reminders.size(), taskId);
    }

    @Transactional
    public void markAsSent(Reminder reminder) {
        reminder.setStatus(Reminder.ReminderStatus.SENT);
        reminder.setSentAt(ZonedDateTime.now());
        reminderRepository.save(reminder);
    }

    @Transactional
    public void markAsFailed(Reminder reminder, String error) {
        reminder.setRetryCount(reminder.getRetryCount() + 1);
        if (reminder.getRetryCount() >= 3) {
            reminder.setStatus(Reminder.ReminderStatus.FAILED);
        }
        reminder.setErrorMessage(error);
        reminderRepository.save(reminder);
    }

    public List<Reminder> getDueReminders() {
        return reminderRepository.findDueReminders(ZonedDateTime.now());
    }

    private record ReminderInterval(long minutesBefore, String label) {}
}
