package com.notivio.repository;

import com.notivio.entity.AiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.UUID;

@Repository
public interface AiLogRepository extends JpaRepository<AiLog, UUID> {

    @Query("SELECT COALESCE(SUM(a.totalTokens), 0) FROM AiLog a " +
           "WHERE a.provider = :provider AND a.createdAt >= :since")
    Long sumTokensByProviderSince(
            @Param("provider") AiLog.AiProvider provider,
            @Param("since") ZonedDateTime since);

    @Query("SELECT COUNT(a) FROM AiLog a WHERE a.provider = :provider " +
           "AND a.createdAt >= :since AND a.success = false")
    Long countFailuresByProviderSince(
            @Param("provider") AiLog.AiProvider provider,
            @Param("since") ZonedDateTime since);
}
