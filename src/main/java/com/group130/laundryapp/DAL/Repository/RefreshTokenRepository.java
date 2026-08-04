package com.group130.laundryapp.DAL.Repository;

import com.group130.laundryapp.Domain.Entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
    public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

        Optional<RefreshToken> findByTokenHash(String tokenHash);

        @Modifying
        @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.account.id = :accountId")
        void revokeAllByAccountId(@Param("accountId") UUID accountId);

        @Modifying
        @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
        void deleteExpiredAndRevoked(@Param("now") OffsetDateTime now);
    }

