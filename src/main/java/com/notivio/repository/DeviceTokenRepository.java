package com.notivio.repository;

import com.notivio.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    List<DeviceToken> findByUserIdAndIsActive(UUID userId, Boolean isActive);

    Optional<DeviceToken> findByUserIdAndToken(UUID userId, String token);

    @Transactional
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.token = :token")
    void deactivateToken(@Param("token") String token);

    boolean existsByUserIdAndToken(UUID userId, String token);
}
