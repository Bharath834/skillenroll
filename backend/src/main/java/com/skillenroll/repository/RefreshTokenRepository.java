package com.skillenroll.repository;

import com.skillenroll.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Data access for {@link RefreshToken}. Database access only - no business logic.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /**
     * Revokes every active refresh token of the given user. Used when token
     * reuse is detected (possible theft) so all sessions are invalidated.
     *
     * @param userId the owning user's id
     * @return number of revoked tokens
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true "
            + "WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllActiveForUser(@Param("userId") Long userId);
}
