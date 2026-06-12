package com.notivio.repository;

import com.notivio.entity.ApiKey;
import com.notivio.entity.ApiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.UUID;

@Repository
public interface ApiUsageLogRepository extends JpaRepository<ApiUsageLog, UUID> {

    @Query("SELECT COALESCE(SUM(u.tokensUsed), 0) FROM ApiUsageLog u " +
           "WHERE u.service = :service AND u.createdAt >= :since")
    Long sumTokensByServiceSince(
            @Param("service") ApiKey.ApiService service,
            @Param("since") ZonedDateTime since);

    @Query("SELECT COALESCE(SUM(u.requestCount), 0) FROM ApiUsageLog u " +
           "WHERE u.service = :service AND u.createdAt >= :since")
    Long sumRequestsByServiceSince(
            @Param("service") ApiKey.ApiService service,
            @Param("since") ZonedDateTime since);
}
