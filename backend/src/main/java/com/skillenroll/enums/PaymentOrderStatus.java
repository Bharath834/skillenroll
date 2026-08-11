package com.skillenroll.enums;

/**
 * Lifecycle of a persisted {@code payment_orders} record.
 *
 * <ul>
 *   <li>{@code CREATED} — the Razorpay order was created but no payment has
 *       been verified yet (the user may still be paying).</li>
 *   <li>{@code PAID} — a payment was captured and its signature verified
 *       server-side.</li>
 * </ul>
 */
public enum PaymentOrderStatus {
    CREATED,
    PAID
}
