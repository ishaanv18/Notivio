package com.notivio.repository;

import com.notivio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.gmailConnected = true")
    List<User> findAllActiveGmailConnectedUsers();

    @Query("SELECT u FROM User u WHERE u.digestEnabled = true AND u.isActive = true")
    List<User> findAllDigestEnabledUsers();
}
