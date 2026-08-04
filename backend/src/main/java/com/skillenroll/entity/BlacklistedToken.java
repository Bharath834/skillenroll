package com.skillenroll.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A blacklisted JWT access token.
 *
 * <p>JWTs are stateless and remain valid until their natural {@code exp}, so
 * logout records the token's SHA-256 hash here to reject it early. Only the
 * hash is stored (never the token itself, which carries profile claims), and
 * {@code expiresAt} mirrors the token's {@code exp} so stale entries can be
 * purged later.
 */
@Entity
@Table(name = "blacklisted_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_blacklisted_tokens_token_hash", columnNames = "token_hash"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 hex digest of the raw JWT. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /** When the underlying JWT expires - after this point the row is useless. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
