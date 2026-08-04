package com.skillenroll.repository;

import com.skillenroll.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link BlacklistedToken}. Database access only - no business logic.
 */
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    boolean existsByTokenHash(String tokenHash);
}
