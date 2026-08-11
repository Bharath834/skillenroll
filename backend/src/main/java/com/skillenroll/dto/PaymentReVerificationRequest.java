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
 * Request payload for re-verifying a previously captured payment.
 *
 * <p>Used when the initial verification attempt failed (e.g. a transient
 * error) after the payment was actually captured. The client only needs the
 * order id — the payment id and signature are read from the persisted order
 * record, so no new order is created and no double charge can occur.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for re-verifying a payment. Only the order id and course are needed — "
        + "the payment id and signature are read from the persisted order record.")
public class PaymentReVerificationRequest {

    @NotBlank(message = "Razorpay order id is required")
    @Schema(description = "Razorpay order id (razorpay_order_id) of the order to re-verify",
            example = "order_P1abcXYZ123def")
    private String orderId;

    @Positive(message = "Course ID must be a positive number")
    @Schema(description = "ID of the purchased course; used to validate the order binding and to "
            + "activate the authenticated user's PENDING enrollment", example = "1")
    private Long courseId;
}
