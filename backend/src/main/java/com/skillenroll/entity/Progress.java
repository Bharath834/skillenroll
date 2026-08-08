package com.skillenroll.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tracks an enrolled {@link User}'s progress in a {@link Course}.
 * A user can have only one progress record per course
 * (uk_progress_user_course). {@code completed} flips to {@code true} and
 * {@code completedAt} is populated once progress reaches 100%.
 */
@Entity
@Table(name = "progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_progress_user_course",
                        columnNames = {"user_id", "course_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Progress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Overall course progress, 0.00 to 100.00. */
    @Column(name = "progress_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal progressPercentage;

    /** False until progress reaches 100; then true with completedAt populated. */
    @Column(nullable = false)
    private boolean completed;

    /** When the learner started the course. */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** When the course was completed (null until progress reaches 100). */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Owning side of the User <-> Progress relationship.
     * {@code @JsonIgnore} prevents circular JSON serialization.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    /**
     * Owning side of the Course <-> Progress relationship.
     * {@code @JsonIgnore} prevents circular JSON serialization.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.startedAt == null) {
            this.startedAt = now;
        }
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
