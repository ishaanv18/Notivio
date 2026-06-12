package com.notivio.repository;

import com.notivio.entity.GmailToken;
import com.notivio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GmailTokenRepository extends JpaRepository<GmailToken, UUID> {

    Optional<GmailToken> findByUser(User user);

    Optional<GmailToken> findByUserId(UUID userId);

    void deleteByUser(User user);

    @Query("SELECT t FROM GmailToken t WHERE t.expiresAt < :threshold")
    List<GmailToken> findTokensExpiringBefore(ZonedDateTime threshold);
}
