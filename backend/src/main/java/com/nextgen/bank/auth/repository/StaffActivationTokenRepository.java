package com.nextgen.bank.auth.repository;

import com.nextgen.bank.auth.domain.StaffActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffActivationTokenRepository extends JpaRepository<StaffActivationToken, UUID> {

    Optional<StaffActivationToken> findByTokenHash(String tokenHash);

    Optional<StaffActivationToken> findByTokenHashAndIsUsedFalse(String tokenHash);

    Optional<StaffActivationToken> findByUserIdAndIsUsedFalse(UUID userId);

    @Modifying
    @Query("UPDATE StaffActivationToken t SET t.isUsed = true, t.usedAt = :usedAt WHERE t.tokenHash = :tokenHash AND t.isUsed = false AND t.expiresAt > :now")
    int consumeToken(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now,
            @Param("usedAt") Instant usedAt
    );

    @Modifying
    @Query("UPDATE StaffActivationToken t SET t.isUsed = true, t.usedAt = :now WHERE t.userId = :userId AND t.isUsed = false")
    int invalidateTokensForUser(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );
}
