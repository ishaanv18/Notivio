package com.notivio.repository;

import com.notivio.entity.ApiAlert;
import com.notivio.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiAlertRepository extends JpaRepository<ApiAlert, UUID> {

    List<ApiAlert> findByStatusOrderByCreatedAtDesc(ApiAlert.AlertStatus status);

    @Query("SELECT a FROM ApiAlert a WHERE a.service = :service " +
           "AND a.status = 'ACTIVE' ORDER BY a.createdAt DESC")
    List<ApiAlert> findActiveAlertsByService(ApiKey.ApiService service);

    boolean existsByServiceAndStatusAndAlertLevel(
            ApiKey.ApiService service,
            ApiAlert.AlertStatus status,
            ApiAlert.AlertLevel alertLevel);
}
