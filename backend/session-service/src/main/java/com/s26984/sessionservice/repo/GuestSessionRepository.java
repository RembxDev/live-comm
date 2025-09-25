package com.s26984.sessionservice.repo;

import com.s26984.sessionservice.model.GuestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestSessionRepository extends JpaRepository<GuestSession, UUID> {

    @Modifying
    @Query("DELETE FROM GuestSession g WHERE g.verified = false AND g.expiresAt < :now")
    int deleteAllExpiredUnverifiedSessions(@Param("now") Instant now);

    Optional<GuestSession> findTop1ByEmailAndVerifiedIsFalseOrderByCreatedAtDesc(String email);
}
