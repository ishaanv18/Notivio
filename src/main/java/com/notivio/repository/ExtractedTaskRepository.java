package com.notivio.repository;

import com.notivio.entity.ExtractedTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExtractedTaskRepository extends JpaRepository<ExtractedTask, UUID> {

    Page<ExtractedTask> findByUserIdOrderByDeadlineAsc(UUID userId, Pageable pageable);

    List<ExtractedTask> findByUserIdAndStatus(UUID userId, ExtractedTask.TaskStatus status);

    @Query("SELECT t FROM ExtractedTask t WHERE t.user.id = :userId " +
           "AND t.deadline BETWEEN :start AND :end " +
           "AND t.status NOT IN ('COMPLETED', 'CANCELLED') " +
           "ORDER BY t.deadline ASC")
    List<ExtractedTask> findUpcomingDeadlines(
            @Param("userId") UUID userId,
            @Param("start") ZonedDateTime start,
            @Param("end") ZonedDateTime end);

    @Query("SELECT t FROM ExtractedTask t WHERE t.user.id = :userId " +
           "AND t.deadline < :now " +
           "AND t.status = 'PENDING'")
    List<ExtractedTask> findOverdueTasks(
            @Param("userId") UUID userId,
            @Param("now") ZonedDateTime now);

    @Query("SELECT t FROM ExtractedTask t WHERE t.user.id = :userId " +
           "AND t.isReminderCreated = false " +
           "AND t.deadline IS NOT NULL " +
           "AND t.deadline > :now " +
           "AND t.status = 'PENDING'")
    List<ExtractedTask> findTasksNeedingReminders(
            @Param("userId") UUID userId,
            @Param("now") ZonedDateTime now);

    /** Duplicate detection: same user, similar title, same deadline window */
    @Query("SELECT COUNT(t) > 0 FROM ExtractedTask t WHERE t.user.id = :userId " +
           "AND LOWER(t.title) = LOWER(:title) " +
           "AND t.deadline BETWEEN :deadlineStart AND :deadlineEnd " +
           "AND t.isDuplicate = false")
    boolean existsSimilarTask(
            @Param("userId") UUID userId,
            @Param("title") String title,
            @Param("deadlineStart") ZonedDateTime deadlineStart,
            @Param("deadlineEnd") ZonedDateTime deadlineEnd);

    @Query("SELECT t FROM ExtractedTask t WHERE t.status = 'PENDING' " +
           "AND t.deadline < :now")
    List<ExtractedTask> findAllPendingOverdue(@Param("now") ZonedDateTime now);

    long countByUserIdAndStatus(UUID userId, ExtractedTask.TaskStatus status);
}
