package com.notivio.repository;

import com.notivio.entity.Email;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailRepository extends JpaRepository<Email, UUID> {

    boolean existsByUserIdAndGmailMessageId(UUID userId, String gmailMessageId);

    Optional<Email> findByUserIdAndGmailMessageId(UUID userId, String gmailMessageId);

    @Query("SELECT e FROM Email e WHERE e.user.id = :userId AND e.isProcessed = false ORDER BY e.receivedAt DESC")
    List<Email> findUnprocessedByUserId(@Param("userId") UUID userId);

    /** Emails where AI analysis failed (error set, not processed yet) */
    @Query("SELECT e FROM Email e WHERE e.user.id = :userId AND e.isProcessed = false AND e.processingError IS NOT NULL ORDER BY e.receivedAt DESC")
    List<Email> findFailedByUserId(@Param("userId") UUID userId);

    /** Emails incorrectly marked as processed but with an error (legacy bug) */
    @Query("SELECT e FROM Email e WHERE e.user.id = :userId AND e.isProcessed = true AND e.processingError IS NOT NULL ORDER BY e.receivedAt DESC")
    List<Email> findProcessedWithErrorsByUserId(@Param("userId") UUID userId);

    @Query("SELECT e FROM Email e WHERE e.user.id = :userId ORDER BY e.receivedAt DESC")
    Page<Email> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    long countByUserIdAndIsProcessed(UUID userId, Boolean isProcessed);
}
