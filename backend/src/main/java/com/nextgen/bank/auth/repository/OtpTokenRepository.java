package com.nextgen.bank.auth.repository;

import com.nextgen.bank.auth.domain.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    @Query("SELECT o FROM OtpToken o WHERE o.identifier = :identifier AND o.purpose = :purpose AND o.isUsed = false ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpToken> findActiveOtp(
            @Param("identifier") String identifier,
            @Param("purpose") String purpose
    );

    @Modifying
    @Query("UPDATE OtpToken o SET o.isUsed = true, o.usedAt = :now WHERE o.identifier = :identifier AND o.purpose = :purpose AND o.isUsed = false")
    int invalidatePreviousOtps(
            @Param("identifier") String identifier,
            @Param("purpose") String purpose,
            @Param("now") Instant now
    );
}
