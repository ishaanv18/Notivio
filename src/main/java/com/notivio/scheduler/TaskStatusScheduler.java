package com.notivio.scheduler;

import com.notivio.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Updates task statuses automatically.
 * Marks PENDING tasks as OVERDUE when their deadline has passed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskStatusScheduler {

    private final TaskService taskService;

    // Every hour
    @Scheduled(cron = "0 0 * * * *")
    public void markOverdueTasks() {
        log.debug("Running overdue task check...");
        taskService.markOverdueTasks();
    }
}
