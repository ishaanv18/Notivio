package com.notivio.repository;

import com.notivio.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    List<Reminder> findByTaskId(UUID taskId);

    List<Reminder> findByUserIdAndStatus(UUID userId, Reminder.ReminderStatus status);

    /** Core scheduler query: find all due reminders */
    @Query("SELECT r FROM Reminder r WHERE r.status = 'SCHEDULED' " +
           "AND r.remindAt <= :now " +
           "ORDER BY r.remindAt ASC")
    List<Reminder> findDueReminders(@Param("now") ZonedDateTime now);

    void deleteByTaskId(UUID taskId);

    boolean existsByTaskIdAndIntervalLabel(UUID taskId, String intervalLabel);

    long countByUserIdAndStatus(UUID userId, Reminder.ReminderStatus status);
}
