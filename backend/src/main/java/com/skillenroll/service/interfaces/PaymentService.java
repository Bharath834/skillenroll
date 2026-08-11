package com.skillenroll.service.interfaces;

import com.razorpay.RazorpayException;
import com.skillenroll.dto.PaymentOrderRequest;
import com.skillenroll.dto.PaymentOrderResponse;
import com.skillenroll.dto.PaymentReVerificationRequest;
import com.skillenroll.dto.PaymentVerificationRequest;
import com.skillenroll.dto.PaymentVerificationResponse;

/**
 * Payment-domain service: Razorpay order creation, payment verification and
 * idempotent re-verification.
 */
public interface PaymentService {

    /**
     * Creates a Razorpay order for a course. The amount is always derived
     * from the course's server-side price, never from the request. The order
     * is persisted (order id, owner, course, amount) so verification can be
     * bound to the database; an existing unpaid order for the same user and
     * course is reused instead of creating a duplicate.
     *
     * @throws RazorpayException when the Razorpay API call fails (mapped to
     *                           {@code 502 Bad Gateway} by the global handler)
     */
    PaymentOrderResponse createOrder(PaymentOrderRequest request) throws RazorpayException;

    /**
     * Verifies a Razorpay payment signature (HMAC-SHA256 of
     * {@code orderId|paymentId} signed with the key secret). The order is
     * looked up in the persisted ledger and bound to the authenticated user
     * and the claimed course before the signature is checked — no Razorpay
     * fetch is needed. On success the order is marked PAID (payment id +
     * signature stored) and the authenticated user's PENDING enrollment for
     * the course is activated.
     *
     * @throws IllegalArgumentException when the order is missing, belongs to
     *                                  another user, does not match the
     *                                  course, or the signature is invalid /
     *                                  data is malformed (mapped to {@code 400})
     */
    PaymentVerificationResponse verifyPayment(PaymentVerificationRequest request);

    /**
     * Re-verifies a previously captured payment using only the persisted order
     * record (no payment id or signature needed from the client, no new
     * Razorpay order) and retries the enrollment activation. Fails with
     * {@code 409} when the order has no verified payment recorded.
     *
     * @throws DuplicateResourceException when the order is not PAID (mapped to
     *                                    {@code 409 Conflict})
     * @throws IllegalArgumentException   when the order is missing, belongs to
     *                                    another user, or does not match the
     *                                    claimed course (mapped to {@code 400})
     */
    PaymentVerificationResponse reVerifyPayment(PaymentReVerificationRequest request);
}
