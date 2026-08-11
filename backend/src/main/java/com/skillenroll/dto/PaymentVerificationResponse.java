package com.skillenroll.dto;

import com.skillenroll.enums.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload after a Razorpay payment is verified.
 *
 * <p>Returned only for valid signatures (a failed verification surfaces as a
 * {@code 400} response), so {@code verified} is always {@code true} here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payment verification result")
public class PaymentVerificationResponse {

    @Schema(description = "Always true - invalid signatures are rejected with HTTP 400", example = "true")
    private boolean verified;

    @Schema(description = "Verified Razorpay order id", example = "order_P1abcXYZ123def")
    private String orderId;

    @Schema(description = "Verified Razorpay payment id", example = "pay_9A2B3C4D5E6F7G")
    private String paymentId;

    @Schema(description = "Enrollment status after verification (null when no enrollment was activated)",
            example = "ACTIVE")
    private EnrollmentStatus enrollmentStatus;
}
