package com.skillenroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for verifying a Razorpay payment after checkout.
 *
 * <p>The three fields mirror the values the Razorpay Checkout returns to the
 * client ({@code razorpay_order_id}, {@code razorpay_payment_id},
 * {@code razorpay_signature}). The signature is validated server-side with the
 * configured key secret; {@code courseId} is optional and, when present,
 * triggers activation of the authenticated user's PENDING enrollment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for verifying a Razorpay payment. orderId, paymentId and signature "
        + "are the values returned by the Razorpay Checkout; courseId is optional and only used to "
        + "activate the authenticated user's PENDING enrollment on success.")
public class PaymentVerificationRequest {

    @NotBlank(message = "Razorpay order id is required")
    @Schema(description = "Razorpay order id (razorpay_order_id) returned by the checkout",
            example = "order_P1abcXYZ123def")
    private String orderId;

    @NotBlank(message = "Razorpay payment id is required")
    @Schema(description = "Razorpay payment id (razorpay_payment_id) returned by the checkout",
            example = "pay_9A2B3C4D5E6F7G")
    private String paymentId;

    @NotBlank(message = "Razorpay signature is required")
    @Schema(description = "Razorpay signature (razorpay_signature) returned by the checkout",
            example = "0d14a7f2c8e1b9d3f6a4c2e8b1d5f7a9c3e6d0b2f4a8c1e3b5d7f9a0c2e4b6d8")
    private String signature;

    @Positive(message = "Course ID must be a positive number")
    @Schema(description = "ID of the purchased course. When present, the authenticated user's PENDING "
            + "enrollment for this course is activated after the signature is verified.",
            example = "1")
    private Long courseId;
}
