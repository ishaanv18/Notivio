package com.notivio.controller;

import com.notivio.entity.ExtractedTask;
import com.notivio.entity.User;
import com.notivio.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Task management API endpoints.
 * All endpoints require JWT authentication.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task and deadline management")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Get all tasks (paginated)")
    public ResponseEntity<Page<ExtractedTask>> getAllTasks(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("deadline").ascending());
        return ResponseEntity.ok(taskService.getAllTasks(user.getId(), pageable));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming deadlines (next N days)")
    public ResponseEntity<List<ExtractedTask>> getUpcoming(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(taskService.getUpcomingDeadlines(user.getId(), days));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue tasks")
    public ResponseEntity<List<ExtractedTask>> getOverdue(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskService.getOverdueTasks(user.getId()));
    }

    @GetMapping("/completed")
    @Operation(summary = "Get completed tasks")
    public ResponseEntity<List<ExtractedTask>> getCompleted(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskService.getTasksByStatus(
                user.getId(), ExtractedTask.TaskStatus.COMPLETED));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<ExtractedTask> getTask(
            @AuthenticationPrincipal User user,
            @PathVariable UUID taskId) {
        return ResponseEntity.ok(taskService.getTaskById(taskId, user.getId()));
    }

    @PatchMapping("/{taskId}/complete")
    @Operation(summary = "Mark task as complete")
    public ResponseEntity<ExtractedTask> markComplete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID taskId) {
        return ResponseEntity.ok(taskService.markComplete(taskId, user.getId()));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Update task details")
    public ResponseEntity<ExtractedTask> updateTask(
            @AuthenticationPrincipal User user,
            @PathVariable UUID taskId,
            @RequestBody ExtractedTask updates) {
        return ResponseEntity.ok(taskService.updateTask(taskId, user.getId(), updates));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete task and its reminders")
    public ResponseEntity<Map<String, String>> deleteTask(
            @AuthenticationPrincipal User user,
            @PathVariable UUID taskId) {
        taskService.deleteTask(taskId, user.getId());
        return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get task statistics for the user")
    public ResponseEntity<Map<String, Long>> getStats(
            @AuthenticationPrincipal User user) {
        UUID uid = user.getId();
        return ResponseEntity.ok(Map.of(
                "pending",   taskService.countByStatus(uid, ExtractedTask.TaskStatus.PENDING),
                "completed", taskService.countByStatus(uid, ExtractedTask.TaskStatus.COMPLETED),
                "overdue",   taskService.countByStatus(uid, ExtractedTask.TaskStatus.OVERDUE),
                "upcoming",  (long) taskService.getUpcomingDeadlines(uid, 7).size()
        ));
    }
}
