package com.notivio.service;

import com.notivio.entity.ExtractedTask;
import com.notivio.repository.ExtractedTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Task management service — CRUD, filtering, status updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final ExtractedTaskRepository taskRepository;
    private final ReminderService reminderService;
    private final CalendarService calendarService;

    public Page<ExtractedTask> getAllTasks(UUID userId, Pageable pageable) {
        return taskRepository.findByUserIdOrderByDeadlineAsc(userId, pageable);
    }

    public List<ExtractedTask> getUpcomingDeadlines(UUID userId, int days) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime end = now.plusDays(days);
        return taskRepository.findUpcomingDeadlines(userId, now, end);
    }

    public List<ExtractedTask> getOverdueTasks(UUID userId) {
        return taskRepository.findOverdueTasks(userId, ZonedDateTime.now());
    }

    public List<ExtractedTask> getTasksByStatus(UUID userId, ExtractedTask.TaskStatus status) {
        return taskRepository.findByUserIdAndStatus(userId, status);
    }

    public ExtractedTask getTaskById(UUID taskId, UUID userId) {
        return taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(userId))
                .orElseThrow(() -> new com.notivio.exception.ResourceNotFoundException(
                        "Task not found: " + taskId));
    }

    @Transactional
    public ExtractedTask markComplete(UUID taskId, UUID userId) {
        ExtractedTask task = getTaskById(taskId, userId);
        task.setStatus(ExtractedTask.TaskStatus.COMPLETED);
        task = taskRepository.save(task);

        // Cancel pending reminders
        reminderService.cancelRemindersForTask(taskId);
        log.info("Task marked complete: {}", task.getTitle());
        return task;
    }

    @Transactional
    public ExtractedTask updateTask(UUID taskId, UUID userId, ExtractedTask updates) {
        ExtractedTask task = getTaskById(taskId, userId);

        if (updates.getTitle() != null)       task.setTitle(updates.getTitle());
        if (updates.getDescription() != null) task.setDescription(updates.getDescription());
        if (updates.getPriority() != null)    task.setPriority(updates.getPriority());
        if (updates.getStatus() != null)      task.setStatus(updates.getStatus());
        if (updates.getDeadline() != null) {
            task.setDeadline(updates.getDeadline());
            // Recreate reminders with new deadline
            reminderService.cancelRemindersForTask(taskId);
            task = taskRepository.save(task);
            reminderService.createRemindersForTask(task);
            task.setIsReminderCreated(true);
        }

        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID userId) {
        ExtractedTask task = getTaskById(taskId, userId);
        reminderService.cancelRemindersForTask(taskId);
        taskRepository.delete(task);
        log.info("Task deleted: {}", taskId);
    }

    @Transactional
    public void markOverdueTasks() {
        List<ExtractedTask> overdue = taskRepository.findAllPendingOverdue(ZonedDateTime.now());
        overdue.forEach(task -> {
            task.setStatus(ExtractedTask.TaskStatus.OVERDUE);
            taskRepository.save(task);
        });
        if (!overdue.isEmpty()) {
            log.info("Marked {} tasks as overdue", overdue.size());
        }
    }

    public long countByStatus(UUID userId, ExtractedTask.TaskStatus status) {
        return taskRepository.countByUserIdAndStatus(userId, status);
    }
}
