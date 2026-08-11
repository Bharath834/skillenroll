package com.skillenroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for creating a Razorpay payment order.
 *
 * <p>The order amount is never taken from the client: it is always derived
 * server-side from the course's stored {@code price} (see
 * {@code PaymentServiceImpl}), so a caller cannot tamper with the charge.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for creating a Razorpay payment order for a course. "
        + "The order amount is always taken from the course's server-side price.")
public class PaymentOrderRequest {

    @NotNull(message = "Course ID is required")
    @Positive(message = "Course ID must be a positive number")
    @Schema(description = "ID of the course being purchased", example = "1")
    private Long courseId;
}
