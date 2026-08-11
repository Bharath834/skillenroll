package com.skillenroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload returned after a Razorpay order is created.
 *
 * <p>Contains everything the upcoming checkout flow needs (order id, amount,
 * currency and the public {@code keyId}) while never exposing the Razorpay
 * key secret.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Razorpay payment order details returned after order creation")
public class PaymentOrderResponse {

    @Schema(description = "Razorpay order id (e.g. order_...)", example = "order_P1abcXYZ123def")
    private String orderId;

    @Schema(description = "ID of the purchased course", example = "1")
    private Long courseId;

    @Schema(description = "Title of the purchased course", example = "Spring Boot Masterclass")
    private String courseTitle;

    @Schema(description = "Order amount in paise (smallest currency unit)", example = "4999")
    private Long amount;

    @Schema(description = "Order currency (ISO 4217)", example = "INR")
    private String currency;

    @Schema(description = "Unique receipt string passed to Razorpay", example = "course-1-1723351234567")
    private String receipt;

    @Schema(description = "Razorpay order status (created/attempted/paid)", example = "created")
    private String status;

    @Schema(description = "Public Razorpay key id used to open the checkout on the client", example = "rzp_test_xxxxxxxx")
    private String keyId;

    @Schema(description = "Order creation time (epoch seconds)", example = "1723351234")
    private Long createdAt;
}
