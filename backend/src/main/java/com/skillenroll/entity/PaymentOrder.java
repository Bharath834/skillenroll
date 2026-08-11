package com.skillenroll.entity;

import com.skillenroll.enums.PaymentOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Server-side ledger of Razorpay orders created through the platform.
 *
 * <p>Stores a snapshot of each order (owner, course, amount in paise,
 * receipt) plus the payment details once a payment is signature-verified
 * (paymentId, signature, status, paidAt). Plain {@code userId}/{@code courseId}
 * columns are used on purpose: the record is a ledger snapshot and must remain
 * valid even if the referenced user/course is later removed.
 *
 * <p>This record lets payment verification validate the order-to-course and
 * order-to-user binding against the database instead of fetching the order
 * from Razorpay, and enables idempotent re-verification after a payment was
 * captured but confirmation failed (no new order, no double charge).
 */
@Entity
@Table(name = "payment_orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_orders_order_id",
                columnNames = "order_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Razorpay order id (e.g. order_...). */
    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    /** Id of the learner who initiated the order (ledger snapshot). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Id of the purchased course (ledger snapshot). */
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /** Order amount in paise (smallest currency unit). */
    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(nullable = false, length = 8)
    private String currency;

    /** Receipt sent to Razorpay at creation time. */
    @Column(nullable = false, length = 50)
    private String receipt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentOrderStatus status;

    /** Razorpay payment id, set once the payment signature is verified. */
    @Column(name = "payment_id", length = 50)
    private String paymentId;

    /** Razorpay signature, set once the payment signature is verified. */
    @Column(length = 128)
    private String signature;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
